package com.auplayer.player.di

import com.auplayer.player.data.SoundRepository
import com.auplayer.player.data.SoundRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideSoundRepository() : SoundRepository{
        return SoundRepositoryImpl()
    }


}