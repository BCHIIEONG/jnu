UPDATE experiment_course_slot
SET first_lesson_date = TIMESTAMPADD(DAY, 7, first_lesson_date),
    lesson_date = TIMESTAMPADD(DAY, 7, lesson_date),
    range_start_date = CASE
        WHEN range_mode = 'DATE_RANGE' AND range_start_date = first_lesson_date THEN TIMESTAMPADD(DAY, 7, range_start_date)
        ELSE range_start_date
    END,
    repeat_pattern = 'ODD_WEEK'
WHERE repeat_pattern = 'EVEN_WEEK';
