package com.herdmanager.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.herdmanager.app.data.local.dao.AnimalDao
import com.herdmanager.app.data.local.dao.BreedingEventDao
import com.herdmanager.app.data.local.dao.CalvingEventDao
import com.herdmanager.app.data.local.dao.HerdAssignmentDao
import com.herdmanager.app.data.local.dao.HerdDao
import com.herdmanager.app.data.local.dao.HealthEventDao
import com.herdmanager.app.data.local.dao.PhotoDao
import com.herdmanager.app.data.local.dao.WeightRecordDao
import com.herdmanager.app.data.local.entity.AnimalEntity
import com.herdmanager.app.data.local.entity.BreedingEventEntity
import com.herdmanager.app.data.local.entity.CalvingEventEntity
import com.herdmanager.app.data.local.entity.HealthEventEntity
import com.herdmanager.app.data.local.entity.HerdAssignmentEntity
import com.herdmanager.app.data.local.entity.HerdEntity
import com.herdmanager.app.data.local.entity.PhotoEntity
import com.herdmanager.app.data.local.entity.WeightRecordEntity

@Database(
    entities = [
        AnimalEntity::class,
        BreedingEventEntity::class,
        CalvingEventEntity::class,
        PhotoEntity::class,
        HealthEventEntity::class,
        WeightRecordEntity::class,
        HerdEntity::class,
        HerdAssignmentEntity::class
    ],
    version = 11,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun animalDao(): AnimalDao
    abstract fun herdDao(): HerdDao
    abstract fun herdAssignmentDao(): HerdAssignmentDao
    abstract fun breedingEventDao(): BreedingEventDao
    abstract fun calvingEventDao(): CalvingEventDao
    abstract fun photoDao(): PhotoDao
    abstract fun healthEventDao(): HealthEventDao
    abstract fun weightRecordDao(): WeightRecordDao
}
