package com.shahid.spendwise.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shahid.spendwise.data.local.SpendWiseDatabase
import com.shahid.spendwise.data.local.dao.*
import com.shahid.spendwise.data.repository.SpendWiseRepository
import com.shahid.spendwise.data.repository.SpendWiseRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideSpendWiseDatabase(
        @ApplicationContext context: Context
    ): SpendWiseDatabase {
        return Room.databaseBuilder(
            context,
            SpendWiseDatabase::class.java,
            SpendWiseDatabase.DATABASE_NAME
        ).addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val prePopulatedDb = Room.databaseBuilder(
                        context,
                        SpendWiseDatabase::class.java,
                        SpendWiseDatabase.DATABASE_NAME
                    ).build()

                    prePopulatedDb.categoryDao().insertAll(SpendWiseDatabase.DEFAULT_CATEGORIES)
                    prePopulatedDb.currencyDao().insertAll(SpendWiseDatabase.DEFAULT_CURRENCIES)
                }
            }
        }).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideTransactionDao(db: SpendWiseDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideCategoryDao(db: SpendWiseDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideBudgetDao(db: SpendWiseDatabase): BudgetDao = db.budgetDao()

    @Provides
    fun provideRecurringDao(db: SpendWiseDatabase): RecurringDao = db.recurringDao()

    @Provides
    fun providePersonDao(db: SpendWiseDatabase): PersonDao = db.personDao()

    @Provides
    fun provideSplitDao(db: SpendWiseDatabase): SplitDao = db.splitDao()

    @Provides
    fun provideDebtDao(db: SpendWiseDatabase): DebtDao = db.debtDao()

    @Provides
    fun provideSavingsGoalDao(db: SpendWiseDatabase): SavingsGoalDao = db.savingsGoalDao()

    @Provides
    fun provideCurrencyDao(db: SpendWiseDatabase): CurrencyDao = db.currencyDao()

    @Provides
    fun provideFilterDao(db: SpendWiseDatabase): SavedFilterDao = db.filterDao()

    @Provides
    @Singleton
    fun provideSpendWiseRepository(
        database: SpendWiseDatabase
    ): SpendWiseRepository = SpendWiseRepositoryImpl(database)
}
