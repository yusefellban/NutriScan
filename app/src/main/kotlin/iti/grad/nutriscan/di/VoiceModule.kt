package iti.grad.nutriscan.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import iti.grad.nutriscan.data.manager.VoiceManagerImpl
import iti.grad.nutriscan.domain.nutrigpt.manager.IVoiceManager

@Module
@InstallIn(ViewModelComponent::class)
abstract class VoiceModule {

    @Binds
    @ViewModelScoped
    abstract fun bindVoiceManager(
        voiceManagerImpl: VoiceManagerImpl
    ): IVoiceManager
}
