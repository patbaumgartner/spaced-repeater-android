package com.patbaumgartner.android.spacedrepeater.di

import android.content.Context
import androidx.work.WorkManager
import com.patbaumgartner.android.spacedrepeater.service.ReviewReminderService
import androidx.room.Room
import com.patbaumgartner.android.spacedrepeater.data.importing.DeckImporter
import com.patbaumgartner.android.spacedrepeater.data.importing.DeckMediaStore
import com.patbaumgartner.android.spacedrepeater.data.importing.MarkdownParser
import com.patbaumgartner.android.spacedrepeater.data.local.DbTransaction
import com.patbaumgartner.android.spacedrepeater.data.local.SpacedRepeaterDatabase
import com.patbaumgartner.android.spacedrepeater.data.local.roomDbTransaction
import com.patbaumgartner.android.spacedrepeater.data.repository.CardRepository
import com.patbaumgartner.android.spacedrepeater.data.repository.DeckRepository
import com.patbaumgartner.android.spacedrepeater.data.repository.AppPreferencesRepository
import com.patbaumgartner.android.spacedrepeater.data.repository.StatsRepository
import com.patbaumgartner.android.spacedrepeater.data.repository.StudyLimitsRepository
import com.patbaumgartner.android.spacedrepeater.data.scheduler.SM2Scheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SpacedRepeaterDatabase =
        Room.databaseBuilder(context, SpacedRepeaterDatabase::class.java, "spaced_repeater.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideDeckDao(db: SpacedRepeaterDatabase) = db.deckDao()

    @Provides
    fun provideCardDao(db: SpacedRepeaterDatabase) = db.cardDao()

    @Provides
    fun provideCardStateDao(db: SpacedRepeaterDatabase) = db.cardStateDao()

    @Provides
    fun provideReviewLogDao(db: SpacedRepeaterDatabase) = db.reviewLogDao()

    @Provides
    @Singleton
    fun provideScheduler(): SM2Scheduler = SM2Scheduler()

    @Provides
    @Singleton
    fun provideDeckMediaStore(@ApplicationContext context: Context): DeckMediaStore =
        DeckMediaStore(File(context.filesDir, "media"))

    @Provides
    @Singleton
    fun provideDeckRepository(
        dao: com.patbaumgartner.android.spacedrepeater.data.local.deck.DeckDao,
        mediaStore: DeckMediaStore,
    ): DeckRepository = DeckRepository(dao, mediaStore)

    @Provides
    @Singleton
    fun provideCardRepository(
        db: SpacedRepeaterDatabase,
        cardDao: com.patbaumgartner.android.spacedrepeater.data.local.card.CardDao,
        stateDao: com.patbaumgartner.android.spacedrepeater.data.local.card.CardStateDao,
        logDao: com.patbaumgartner.android.spacedrepeater.data.local.card.ReviewLogDao,
        scheduler: SM2Scheduler,
        dbTransaction: DbTransaction,
    ): CardRepository = CardRepository(db, cardDao, stateDao, logDao, scheduler, dbTransaction)

    @Provides
    @Singleton
    fun provideDbTransaction(db: SpacedRepeaterDatabase): DbTransaction = roomDbTransaction(db)

    @Provides
    @Singleton
    fun provideStatsRepository(
        logDao: com.patbaumgartner.android.spacedrepeater.data.local.card.ReviewLogDao,
    ): StatsRepository = StatsRepository(logDao)

    @Provides
    @Singleton
    fun provideStudyLimitsRepository(
        @ApplicationContext context: Context,
        logDao: com.patbaumgartner.android.spacedrepeater.data.local.card.ReviewLogDao,
    ): StudyLimitsRepository = StudyLimitsRepository(context, logDao)

    @Provides
    @Singleton
    fun provideAppPreferencesRepository(
        @ApplicationContext context: Context,
    ): AppPreferencesRepository = AppPreferencesRepository(context)

    @Provides
    @Singleton
    fun provideDeckImporter(
        deckRepository: DeckRepository,
        cardRepository: CardRepository,
        mediaStore: DeckMediaStore,
    ): DeckImporter = DeckImporter(
        deckRepository = deckRepository,
        cardRepository = cardRepository,
        parser = MarkdownParser(),
        mediaStore = mediaStore,
    )

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideReviewReminderService(
        @ApplicationContext context: Context,
        workManager: WorkManager,
    ): ReviewReminderService = ReviewReminderService(context, workManager)
}
