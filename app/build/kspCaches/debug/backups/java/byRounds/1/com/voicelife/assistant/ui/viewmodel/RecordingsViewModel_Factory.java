package com.voicelife.assistant.ui.viewmodel;

import android.app.Application;
import com.voicelife.assistant.data.repository.RecordingRepository;
import com.voicelife.assistant.utils.DebugLogger;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class RecordingsViewModel_Factory implements Factory<RecordingsViewModel> {
  private final Provider<Application> applicationProvider;

  private final Provider<RecordingRepository> recordingRepositoryProvider;

  private final Provider<DebugLogger> debugLoggerProvider;

  public RecordingsViewModel_Factory(Provider<Application> applicationProvider,
      Provider<RecordingRepository> recordingRepositoryProvider,
      Provider<DebugLogger> debugLoggerProvider) {
    this.applicationProvider = applicationProvider;
    this.recordingRepositoryProvider = recordingRepositoryProvider;
    this.debugLoggerProvider = debugLoggerProvider;
  }

  @Override
  public RecordingsViewModel get() {
    return newInstance(applicationProvider.get(), recordingRepositoryProvider.get(), debugLoggerProvider.get());
  }

  public static RecordingsViewModel_Factory create(Provider<Application> applicationProvider,
      Provider<RecordingRepository> recordingRepositoryProvider,
      Provider<DebugLogger> debugLoggerProvider) {
    return new RecordingsViewModel_Factory(applicationProvider, recordingRepositoryProvider, debugLoggerProvider);
  }

  public static RecordingsViewModel newInstance(Application application,
      RecordingRepository recordingRepository, DebugLogger debugLogger) {
    return new RecordingsViewModel(application, recordingRepository, debugLogger);
  }
}
