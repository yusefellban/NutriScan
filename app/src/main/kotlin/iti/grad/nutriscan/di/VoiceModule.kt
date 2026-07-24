package iti.grad.nutriscan.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import iti.grad.nutriscan.data.manager.VoiceManagerImpl
import iti.grad.nutriscan.domain.nutrigpt.manager.IVoiceManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceModule {

    @Binds
    @Singleton
    abstract fun bindVoiceManager(
        voiceManagerImpl: VoiceManagerImpl
    ): IVoiceManager
}
