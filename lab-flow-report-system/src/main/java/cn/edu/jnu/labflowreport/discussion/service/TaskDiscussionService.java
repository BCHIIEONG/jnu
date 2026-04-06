package cn.edu.jnu.labflowreport.discussion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.common.api.ApiCode;
import cn.edu.jnu.labflowreport.common.exception.BusinessException;
import cn.edu.jnu.labflowreport.discussion.dto.TaskDiscussionMessageCreateRequest;
import cn.edu.jnu.labflowreport.discussion.dto.TaskDiscussionThreadCreateRequest;
import cn.edu.jnu.labflowreport.discussion.vo.StudentDiscussionUnreadSummaryVO;
import cn.edu.jnu.labflowreport.discussion.vo.TaskDiscussionMessageVO;
import cn.edu.jnu.labflowreport.discussion.vo.TaskDiscussionTaskViewVO;
import cn.edu.jnu.labflowreport.discussion.vo.TaskDiscussionThreadVO;
import cn.edu.jnu.labflowreport.discussion.vo.TaskDiscussionUnreadItemVO;
import cn.edu.jnu.labflowreport.discussion.vo.TeacherDiscussionAggregateItemVO;
import cn.edu.jnu.labflowreport.discussion.vo.UnreadCountVO;
import cn.edu.jnu.labflowreport.persistence.entity.SysUserEntity;
import cn.edu.jnu.labflowreport.persistence.entity.TaskDiscussionMessageEntity;
import cn.edu.jnu.labflowreport.persistence.entity.TaskDiscussionReadStateEntity;
import cn.edu.jnu.labflowreport.persistence.entity.TaskDiscussionThreadEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.SysUserMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.TaskDiscussionMessageMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.TaskDiscussionReadStateMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.TaskDiscussionThreadMapper;
import cn.edu.jnu.labflowreport.workflow.service.ReportWorkflowService;
import cn.edu.jnu.labflowreport.workflow.vo.TaskVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskDiscussionService {

    private static final String TYPE_NORMAL = "NORMAL";
    private static final String TYPE_ASK_TEACHER = "ASK_TEACHER";
    private static final Set<String> VALID_TYPES = Set.of(TYPE_NORMAL, TYPE_ASK_TEACHER);
    private static final Set<String> STAFF_AUTHOR_ROLES = Set.of("TEACHER", "ADMIN");

    private final TaskDiscussionThreadMapper threadMapper;
    private final TaskDiscussionMessageMapper messageMapper;
    private final TaskDiscussionReadStateMapper readStateMapper;
    private final SysUserMapper sysUserMapper;
    private final ReportWorkflowService reportWorkflowService;

    public TaskDiscussionService(
            TaskDiscussionThreadMapper threadMapper,
            TaskDiscussionMessageMapper messageMapper,
            TaskDiscussionReadStateMapper readStateMapper,
            SysUserMapper sysUserMapper,
            ReportWorkflowService reportWorkflowService
    ) {
        this.threadMapper = threadMapper;
        this.messageMapper = messageMapper;
        this.readStateMapper = readStateMapper;
        this.sysUserMapper = sysUserMapper;
        this.reportWorkflowService = reportWorkflowService;
    }

    public TaskDiscussionTaskViewVO getStudentTaskDiscussion(Long taskId, AuthenticatedUser user) {
        ensureStudentCanAccessTask(taskId, user);
        return buildTaskView(taskId, user);
    }

    @Transactional
    public TaskDiscussionThreadVO createStudentThread(Long taskId, AuthenticatedUser user, TaskDiscussionThreadCreateRequest request) {
        ensureStudentCanAccessTask(taskId, user);
        String type = normalizeType(request.type());
        String content = normalizeContent(request.content());
        TaskDiscussionThreadEntity thread = new TaskDiscussionThreadEntity();
        thread.setTaskId(taskId);
        thread.setType(type);
        thread.setCreatorId(user.userId());
        thread.setCreatedAt(LocalDateTime.now());
        thread.setUpdatedAt(thread.getCreatedAt());
        threadMapper.insert(thread);
        TaskDiscussionMessageEntity message = createMessage(thread.getId(), user, content);
        touchThread(thread, message);
        upsertReadState(thread.getId(), user.userId(), message.getId());
        return findThreadVo(taskId, thread.getId(), user);
    }

    @Transactional
    public TaskDiscussionThreadVO replyStudentThread(Long taskId, Long threadId, AuthenticatedUser user, TaskDiscussionMessageCreateRequest request) {
        ensureStudentCanAccessTask(taskId, user);
        TaskDiscussionThreadEntity thread = getThreadForTask(threadId, taskId);
        TaskDiscussionMessageEntity message = createMessage(threadId, user, normalizeContent(request.content()));
        touchThread(thread, message);
        upsertReadState(threadId, user.userId(), message.getId());
        return findThreadVo(taskId, threadId, user);
    }

    @Transactional
    public void markStudentThreadRead(Long taskId, Long threadId, AuthenticatedUser user) {
        ensureStudentCanAccessTask(taskId, user);
        TaskDiscussionThreadEntity thread = getThreadForTask(threadId, taskId);
        markThreadRead(thread, user.userId());
    }

    public StudentDiscussionUnreadSummaryVO getStudentUnreadSummary(AuthenticatedUser user) {
        ensureStudent(user);
        List<TaskVO> tasks = reportWorkflowService.listTasks(user);
        Set<Long> taskIds = tasks.stream().map(TaskVO::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        StudentDiscussionUnreadSummaryVO vo = new StudentDiscussionUnreadSummaryVO();
        if (taskIds.isEmpty()) {
            return vo;
        }
        List<TaskDiscussionThreadEntity> threads = threadMapper.selectList(new LambdaQueryWrapper<TaskDiscussionThreadEntity>()
                .in(TaskDiscussionThreadEntity::getTaskId, taskIds)
                .eq(TaskDiscussionThreadEntity::getType, TYPE_ASK_TEACHER)
                .eq(TaskDiscussionThreadEntity::getCreatorId, user.userId())
                .orderByDesc(TaskDiscussionThreadEntity::getLatestMessageAt)
                .orderByDesc(TaskDiscussionThreadEntity::getId));
        Map<Long, Integer> unreadByTask = computeUnreadByTaskForStudent(threads, user.userId());
        List<TaskDiscussionUnreadItemVO> items = unreadByTask.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .map(entry -> {
                    TaskDiscussionUnreadItemVO item = new TaskDiscussionUnreadItemVO();
                    item.setTaskId(entry.getKey());
                    item.setUnreadCount(entry.getValue());
                    return item;
                })
                .toList();
        vo.setItems(new ArrayList<>(items));
        vo.setTotalUnreadCount(items.stream().mapToInt(TaskDiscussionUnreadItemVO::getUnreadCount).sum());
        return vo;
    }

    public List<TeacherDiscussionAggregateItemVO> getTeacherDiscussionAggregate(AuthenticatedUser user, String q, Long taskId, boolean unreadOnly) {
        ensureTeacherOrAdmin(user);
        List<TaskVO> visibleTasks = reportWorkflowService.listTasks(user);
        Map<Long, TaskVO> taskMap = visibleTasks.stream()
                .filter(task -> task.getId() != null)
                .collect(Collectors.toMap(TaskVO::getId, task -> task, (a, b) -> a, LinkedHashMap::new));
        if (taskId != null) {
            reportWorkflowService.getTaskForUser(taskId, user);
            taskMap.entrySet().removeIf(entry -> !Objects.equals(entry.getKey(), taskId));
        }
        if (taskMap.isEmpty()) {
            return List.of();
        }
        List<TaskDiscussionThreadEntity> threads = threadMapper.selectList(new LambdaQueryWrapper<TaskDiscussionThreadEntity>()
                .in(TaskDiscussionThreadEntity::getTaskId, taskMap.keySet())
                .eq(TaskDiscussionThreadEntity::getType, TYPE_ASK_TEACHER)
                .orderByDesc(TaskDiscussionThreadEntity::getLatestMessageAt)
                .orderByDesc(TaskDiscussionThreadEntity::getId));
        if (threads.isEmpty()) {
            return List.of();
        }
        Map<Long, List<TaskDiscussionMessageEntity>> messagesByThread = loadMessagesByThread(threads.stream().map(TaskDiscussionThreadEntity::getId).toList());
        Map<Long, TaskDiscussionReadStateEntity> readStates = loadReadStatesByThread(threads.stream().map(TaskDiscussionThreadEntity::getId).toList(), user.userId());
        Map<Long, SysUserEntity> users = loadUsers(extractUserIds(threads, messagesByThread));
        String keyword = normalizeKeyword(q);
        List<TeacherDiscussionAggregateItemVO> items = new ArrayList<>();
        for (TaskDiscussionThreadEntity thread : threads) {
            List<TaskDiscussionMessageEntity> messages = messagesByThread.getOrDefault(thread.getId(), List.of());
            SysUserEntity creator = users.get(thread.getCreatorId());
            int unreadCount = computeUnreadCountForTeacher(thread, messages, readStates.get(thread.getId()));
            if (unreadOnly && unreadCount <= 0) {
                continue;
            }
            TeacherDiscussionAggregateItemVO item = new TeacherDiscussionAggregateItemVO();
            item.setThreadId(thread.getId());
            item.setTaskId(thread.getTaskId());
            item.setTaskTitle(taskMap.get(thread.getTaskId()) == null ? "任务" : taskMap.get(thread.getTaskId()).getTitle());
            item.setStudentId(thread.getCreatorId());
            item.setStudentUsername(creator == null ? "-" : creator.getUsername());
            item.setStudentDisplayName(creator == null ? "-" : creator.getDisplayName());
            item.setLatestMessagePreview(toPreview(messages.isEmpty() ? null : messages.get(messages.size() - 1).getContent()));
            item.setLatestTeacherReplyPreview(toPreview(findLatestTeacherReply(messages)));
            item.setLatestMessageAt(thread.getLatestMessageAt());
            item.setUnreadCount(unreadCount);
            if (!keyword.isEmpty() && !matchesTeacherAggregateKeyword(item, keyword)) {
                continue;
            }
            items.add(item);
        }
        items.sort(Comparator.comparing(TeacherDiscussionAggregateItemVO::getLatestMessageAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return items;
    }

    public UnreadCountVO getTeacherUnreadSummary(AuthenticatedUser user) {
        ensureTeacherOrAdmin(user);
        List<TeacherDiscussionAggregateItemVO> items = getTeacherDiscussionAggregate(user, null, null, true);
        UnreadCountVO vo = new UnreadCountVO();
        vo.setUnreadCount(items.stream().mapToInt(TeacherDiscussionAggregateItemVO::getUnreadCount).sum());
        return vo;
    }

    public TaskDiscussionTaskViewVO getTeacherTaskDiscussion(Long taskId, AuthenticatedUser user) {
        ensureTeacherOrAdminCanAccessTask(taskId, user);
        return buildTaskView(taskId, user);
    }

    @Transactional
    public TaskDiscussionThreadVO replyTeacherThread(Long taskId, Long threadId, AuthenticatedUser user, TaskDiscussionMessageCreateRequest request) {
        ensureTeacherOrAdminCanAccessTask(taskId, user);
        TaskDiscussionThreadEntity thread = getThreadForTask(threadId, taskId);
        TaskDiscussionMessageEntity message = createMessage(threadId, user, normalizeContent(request.content()));
        touchThread(thread, message);
        upsertReadState(threadId, user.userId(), message.getId());
        return findThreadVo(taskId, threadId, user);
    }

    @Transactional
    public void markTeacherThreadRead(Long taskId, Long threadId, AuthenticatedUser user) {
        ensureTeacherOrAdminCanAccessTask(taskId, user);
        TaskDiscussionThreadEntity thread = getThreadForTask(threadId, taskId);
        markThreadRead(thread, user.userId());
    }

    private TaskDiscussionThreadVO findThreadVo(Long taskId, Long threadId, AuthenticatedUser user) {
        TaskDiscussionTaskViewVO view = buildTaskView(taskId, user);
        return view.getTeacherQaThreads().stream()
                .filter(item -> Objects.equals(item.getId(), threadId))
                .findFirst()
                .or(() -> view.getNormalThreads().stream().filter(item -> Objects.equals(item.getId(), threadId)).findFirst())
                .orElseThrow(() -> new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "讨论主题不存在"));
    }

    private TaskDiscussionTaskViewVO buildTaskView(Long taskId, AuthenticatedUser user) {
        TaskVO task = reportWorkflowService.getTaskForUser(taskId, user);
        List<TaskDiscussionThreadEntity> threads = threadMapper.selectList(new LambdaQueryWrapper<TaskDiscussionThreadEntity>()
                .eq(TaskDiscussionThreadEntity::getTaskId, taskId)
                .orderByDesc(TaskDiscussionThreadEntity::getLatestMessageAt)
                .orderByDesc(TaskDiscussionThreadEntity::getId));
        TaskDiscussionTaskViewVO view = new TaskDiscussionTaskViewVO();
        if (threads.isEmpty()) {
            return view;
        }
        List<Long> threadIds = threads.stream().map(TaskDiscussionThreadEntity::getId).toList();
        Map<Long, List<TaskDiscussionMessageEntity>> messagesByThread = loadMessagesByThread(threadIds);
        Map<Long, TaskDiscussionReadStateEntity> readStates = loadReadStatesByThread(threadIds, user.userId());
        Map<Long, SysUserEntity> users = loadUsers(extractUserIds(threads, messagesByThread));
        for (TaskDiscussionThreadEntity thread : threads) {
            TaskDiscussionThreadVO threadVo = toThreadVo(thread, task, messagesByThread.getOrDefault(thread.getId(), List.of()), readStates.get(thread.getId()), users, user);
            if (TYPE_ASK_TEACHER.equals(thread.getType())) {
                view.getTeacherQaThreads().add(threadVo);
                view.setUnreadTeacherReplyCount(view.getUnreadTeacherReplyCount() + threadVo.getUnreadCount());
            } else {
                view.getNormalThreads().add(threadVo);
            }
        }
        return view;
    }

    private TaskDiscussionThreadVO toThreadVo(TaskDiscussionThreadEntity thread, TaskVO task, List<TaskDiscussionMessageEntity> messages, TaskDiscussionReadStateEntity readState, Map<Long, SysUserEntity> users, AuthenticatedUser viewer) {
        TaskDiscussionThreadVO vo = new TaskDiscussionThreadVO();
        vo.setId(thread.getId());
        vo.setTaskId(thread.getTaskId());
        vo.setTaskTitle(task.getTitle());
        vo.setType(thread.getType());
        vo.setCreatorId(thread.getCreatorId());
        SysUserEntity creator = users.get(thread.getCreatorId());
        vo.setCreatorUsername(creator == null ? "-" : creator.getUsername());
        vo.setCreatorDisplayName(creator == null ? "-" : creator.getDisplayName());
        vo.setLatestMessageAt(thread.getLatestMessageAt());
        if (!messages.isEmpty()) {
            vo.setLatestMessagePreview(toPreview(messages.get(messages.size() - 1).getContent()));
        }
        TaskDiscussionMessageEntity latestTeacherReply = findLatestTeacherReplyEntity(messages);
        if (latestTeacherReply != null) {
            vo.setLatestTeacherReplyPreview(toPreview(latestTeacherReply.getContent()));
            vo.setLatestTeacherReplyAt(latestTeacherReply.getCreatedAt());
        }
        vo.setUnreadCount(computeUnreadCount(thread, messages, readState, viewer));
        for (TaskDiscussionMessageEntity message : messages) {
            TaskDiscussionMessageVO messageVo = new TaskDiscussionMessageVO();
            messageVo.setId(message.getId());
            messageVo.setThreadId(message.getThreadId());
            messageVo.setAuthorId(message.getAuthorId());
            SysUserEntity author = users.get(message.getAuthorId());
            messageVo.setAuthorUsername(author == null ? "-" : author.getUsername());
            messageVo.setAuthorDisplayName(author == null ? "-" : author.getDisplayName());
            messageVo.setAuthorRole(message.getAuthorRole());
            messageVo.setContent(message.getContent());
            messageVo.setCreatedAt(message.getCreatedAt());
            vo.getMessages().add(messageVo);
        }
        return vo;
    }

    private int computeUnreadCount(TaskDiscussionThreadEntity thread, List<TaskDiscussionMessageEntity> messages, TaskDiscussionReadStateEntity readState, AuthenticatedUser viewer) {
        if (hasTeacherRole(viewer)) {
            return computeUnreadCountForTeacher(thread, messages, readState);
        }
        return computeUnreadCountForStudent(thread, messages, readState, viewer.userId());
    }

    private int computeUnreadCountForTeacher(TaskDiscussionThreadEntity thread, List<TaskDiscussionMessageEntity> messages, TaskDiscussionReadStateEntity readState) {
        if (!TYPE_ASK_TEACHER.equals(thread.getType())) {
            return 0;
        }
        long lastReadId = readState == null || readState.getLastReadMessageId() == null ? 0L : readState.getLastReadMessageId();
        int count = 0;
        for (TaskDiscussionMessageEntity message : messages) {
            if (message.getId() != null && message.getId() > lastReadId && "STUDENT".equals(message.getAuthorRole())) {
                count++;
            }
        }
        return count;
    }

    private int computeUnreadCountForStudent(TaskDiscussionThreadEntity thread, List<TaskDiscussionMessageEntity> messages, TaskDiscussionReadStateEntity readState, Long studentId) {
        if (!TYPE_ASK_TEACHER.equals(thread.getType()) || !Objects.equals(thread.getCreatorId(), studentId)) {
            return 0;
        }
        long lastReadId = readState == null || readState.getLastReadMessageId() == null ? 0L : readState.getLastReadMessageId();
        int count = 0;
        for (TaskDiscussionMessageEntity message : messages) {
            if (message.getId() != null && message.getId() > lastReadId && STAFF_AUTHOR_ROLES.contains(message.getAuthorRole())) {
                count++;
            }
        }
        return count;
    }

    private Map<Long, Integer> computeUnreadByTaskForStudent(List<TaskDiscussionThreadEntity> threads, Long studentId) {
        if (threads.isEmpty()) {
            return Map.of();
        }
        List<Long> threadIds = threads.stream().map(TaskDiscussionThreadEntity::getId).toList();
        Map<Long, List<TaskDiscussionMessageEntity>> messagesByThread = loadMessagesByThread(threadIds);
        Map<Long, TaskDiscussionReadStateEntity> readStates = loadReadStatesByThread(threadIds, studentId);
        Map<Long, Integer> result = new HashMap<>();
        for (TaskDiscussionThreadEntity thread : threads) {
            int unread = computeUnreadCountForStudent(thread, messagesByThread.getOrDefault(thread.getId(), List.of()), readStates.get(thread.getId()), studentId);
            if (unread > 0) {
                result.merge(thread.getTaskId(), unread, Integer::sum);
            }
        }
        return result;
    }

    private Map<Long, List<TaskDiscussionMessageEntity>> loadMessagesByThread(List<Long> threadIds) {
        if (threadIds.isEmpty()) {
            return Map.of();
        }
        List<TaskDiscussionMessageEntity> messages = messageMapper.selectList(new LambdaQueryWrapper<TaskDiscussionMessageEntity>()
                .in(TaskDiscussionMessageEntity::getThreadId, threadIds)
                .orderByAsc(TaskDiscussionMessageEntity::getThreadId)
                .orderByAsc(TaskDiscussionMessageEntity::getId));
        return messages.stream().collect(Collectors.groupingBy(TaskDiscussionMessageEntity::getThreadId, LinkedHashMap::new, Collectors.toList()));
    }

    private Map<Long, TaskDiscussionReadStateEntity> loadReadStatesByThread(List<Long> threadIds, Long userId) {
        if (threadIds.isEmpty()) {
            return Map.of();
        }
        List<TaskDiscussionReadStateEntity> list = readStateMapper.selectList(new LambdaQueryWrapper<TaskDiscussionReadStateEntity>()
                .eq(TaskDiscussionReadStateEntity::getUserId, userId)
                .in(TaskDiscussionReadStateEntity::getThreadId, threadIds));
        return list.stream().collect(Collectors.toMap(TaskDiscussionReadStateEntity::getThreadId, item -> item, (a, b) -> a));
    }

    private Map<Long, SysUserEntity> loadUsers(Collection<Long> userIds) {
        Set<Long> ids = userIds.stream().filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
        if (ids.isEmpty()) {
            return Map.of();
        }
        return sysUserMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(SysUserEntity::getId, item -> item, (a, b) -> a));
    }

    private Set<Long> extractUserIds(List<TaskDiscussionThreadEntity> threads, Map<Long, List<TaskDiscussionMessageEntity>> messagesByThread) {
        Set<Long> ids = new HashSet<>();
        for (TaskDiscussionThreadEntity thread : threads) {
            ids.add(thread.getCreatorId());
            for (TaskDiscussionMessageEntity message : messagesByThread.getOrDefault(thread.getId(), List.of())) {
                ids.add(message.getAuthorId());
            }
        }
        return ids;
    }

    private TaskDiscussionMessageEntity createMessage(Long threadId, AuthenticatedUser user, String content) {
        TaskDiscussionMessageEntity message = new TaskDiscussionMessageEntity();
        message.setThreadId(threadId);
        message.setAuthorId(user.userId());
        message.setAuthorRole(resolveAuthorRole(user));
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
        return message;
    }

    private void touchThread(TaskDiscussionThreadEntity thread, TaskDiscussionMessageEntity message) {
        thread.setLatestMessageId(message.getId());
        thread.setLatestMessageAt(message.getCreatedAt());
        thread.setUpdatedAt(message.getCreatedAt());
        threadMapper.updateById(thread);
    }

    private void markThreadRead(TaskDiscussionThreadEntity thread, Long userId) {
        if (thread.getLatestMessageId() == null) {
            return;
        }
        upsertReadState(thread.getId(), userId, thread.getLatestMessageId());
    }

    private void upsertReadState(Long threadId, Long userId, Long lastReadMessageId) {
        TaskDiscussionReadStateEntity existing = readStateMapper.selectOne(new LambdaQueryWrapper<TaskDiscussionReadStateEntity>()
                .eq(TaskDiscussionReadStateEntity::getThreadId, threadId)
                .eq(TaskDiscussionReadStateEntity::getUserId, userId)
                .last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            TaskDiscussionReadStateEntity state = new TaskDiscussionReadStateEntity();
            state.setThreadId(threadId);
            state.setUserId(userId);
            state.setLastReadMessageId(lastReadMessageId);
            state.setLastReadAt(now);
            readStateMapper.insert(state);
            return;
        }
        existing.setLastReadMessageId(lastReadMessageId);
        existing.setLastReadAt(now);
        readStateMapper.updateById(existing);
    }

    private TaskDiscussionThreadEntity getThreadForTask(Long threadId, Long taskId) {
        TaskDiscussionThreadEntity thread = threadMapper.selectById(threadId);
        if (thread == null || !Objects.equals(thread.getTaskId(), taskId)) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "讨论主题不存在");
        }
        return thread;
    }

    private void ensureStudentCanAccessTask(Long taskId, AuthenticatedUser user) {
        ensureStudent(user);
        reportWorkflowService.getTaskForUser(taskId, user);
    }

    private void ensureTeacherOrAdminCanAccessTask(Long taskId, AuthenticatedUser user) {
        ensureTeacherOrAdmin(user);
        reportWorkflowService.getTaskForUser(taskId, user);
    }

    private void ensureStudent(AuthenticatedUser user) {
        if (user == null || user.roleCodes() == null || !user.roleCodes().contains("ROLE_STUDENT")) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "无权访问讨论区");
        }
    }

    private void ensureTeacherOrAdmin(AuthenticatedUser user) {
        if (user == null || user.roleCodes() == null || (!user.roleCodes().contains("ROLE_TEACHER") && !user.roleCodes().contains("ROLE_ADMIN"))) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "无权访问讨论区");
        }
    }

    private boolean hasTeacherRole(AuthenticatedUser user) {
        return user != null && user.roleCodes() != null && (user.roleCodes().contains("ROLE_TEACHER") || user.roleCodes().contains("ROLE_ADMIN"));
    }

    private String normalizeType(String type) {
        String normalized = type == null ? "" : type.trim().toUpperCase();
        if (!VALID_TYPES.contains(normalized)) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "讨论类型不合法");
        }
        return normalized;
    }

    private String normalizeContent(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isBlank()) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "讨论内容不能为空");
        }
        return normalized;
    }

    private String resolveAuthorRole(AuthenticatedUser user) {
        if (user.roleCodes() != null) {
            if (user.roleCodes().contains("ROLE_ADMIN")) {
                return "ADMIN";
            }
            if (user.roleCodes().contains("ROLE_TEACHER")) {
                return "TEACHER";
            }
        }
        return "STUDENT";
    }

    private String toPreview(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String normalized = content.replace('\r', ' ').replace('\n', ' ').trim();
        if (normalized.length() <= 60) {
            return normalized;
        }
        return normalized.substring(0, 60) + "...";
    }

    private TaskDiscussionMessageEntity findLatestTeacherReplyEntity(List<TaskDiscussionMessageEntity> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            TaskDiscussionMessageEntity message = messages.get(i);
            if (STAFF_AUTHOR_ROLES.contains(message.getAuthorRole())) {
                return message;
            }
        }
        return null;
    }

    private String findLatestTeacherReply(List<TaskDiscussionMessageEntity> messages) {
        TaskDiscussionMessageEntity message = findLatestTeacherReplyEntity(messages);
        return message == null ? "" : message.getContent();
    }

    private String normalizeKeyword(String q) {
        return q == null ? "" : q.trim().toLowerCase();
    }

    private boolean matchesTeacherAggregateKeyword(TeacherDiscussionAggregateItemVO item, String keyword) {
        return containsKeyword(item.getTaskTitle(), keyword)
                || containsKeyword(item.getStudentDisplayName(), keyword)
                || containsKeyword(item.getStudentUsername(), keyword)
                || containsKeyword(item.getLatestMessagePreview(), keyword)
                || containsKeyword(item.getLatestTeacherReplyPreview(), keyword);
    }

    private boolean containsKeyword(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }
}
