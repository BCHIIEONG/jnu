<script setup lang="ts">
import LabRoomManager from '../../common/LabRoomManager.vue'
</script>

<template>
  <LabRoomManager
    api-base="/api/admin/lab-rooms"
    export-path="/api/admin/lab-rooms/export"
    export-filename="lab-rooms.csv"
    export-excel-path="/api/admin/lab-rooms/export/excel"
    export-excel-filename="lab-rooms.xlsx"
  />
</template>
