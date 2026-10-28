package com.patbaumgartner.android.spacedrepeater.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.patbaumgartner.android.spacedrepeater.data.local.card.CardDao
import com.patbaumgartner.android.spacedrepeater.data.local.card.CardStateDao
import com.patbaumgartner.android.spacedrepeater.data.local.card.ReviewLogDao
import com.patbaumgartner.android.spacedrepeater.data.local.deck.DeckDao
import com.patbaumgartner.android.spacedrepeater.data.local.model.CardEntity
import com.patbaumgartner.android.spacedrepeater.data.local.model.CardStateEntity
import com.patbaumgartner.android.spacedrepeater.data.local.model.DeckEntity
import com.patbaumgartner.android.spacedrepeater.data.local.model.ReviewLogEntity

@Database(
    entities = [
        DeckEntity::class,
        CardEntity::class,
        CardStateEntity::class,
        ReviewLogEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class SpacedRepeaterDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
    abstract fun cardStateDao(): CardStateDao
    abstract fun reviewLogDao(): ReviewLogDao
}
