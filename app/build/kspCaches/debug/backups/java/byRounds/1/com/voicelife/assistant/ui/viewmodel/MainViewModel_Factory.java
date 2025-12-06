package com.voicelife.assistant.ui.viewmodel;

import android.app.Application;
import com.voicelife.assistant.data.repository.RecordingRepository;
import com.voicelife.assistant.storage.StorageManager;
import com.voicelife.assistant.utils.DebugLogger;
import com.voicelife.assistant.utils.PermissionManager;
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
public final class MainViewModel_Factory implements Factory<MainViewModel> {
  private final Provider<Application> applicationProvider;

  private final Provider<PermissionManager> permissionManagerProvider;

  private final Provider<RecordingRepository> recordingRepositoryProvider;

  private final Provider<StorageManager> storageManagerProvider;

  private final Provider<DebugLogger> debugLoggerProvider;

  public MainViewModel_Factory(Provider<Application> applicationProvider,
      Provider<PermissionManager> permissionManagerProvider,
      Provider<RecordingRepository> recordingRepositoryProvider,
      Provider<StorageManager> storageManagerProvider, Provider<DebugLogger> debugLoggerProvider) {
    this.applicationProvider = applicationProvider;
    this.permissionManagerProvider = permissionManagerProvider;
    this.recordingRepositoryProvider = recordingRepositoryProvider;
    this.storageManagerProvider = storageManagerProvider;
    this.debugLoggerProvider = debugLoggerProvider;
  }

  @Override
  public MainViewModel get() {
    return newInstance(applicationProvider.get(), permissionManagerProvider.get(), recordingRepositoryProvider.get(), storageManagerProvider.get(), debugLoggerProvider.get());
  }

  public static MainViewModel_Factory create(Provider<Application> applicationProvider,
      Provider<PermissionManager> permissionManagerProvider,
      Provider<RecordingRepository> recordingRepositoryProvider,
      Provider<StorageManager> storageManagerProvider, Provider<DebugLogger> debugLoggerProvider) {
    return new MainViewModel_Factory(applicationProvider, permissionManagerProvider, recordingRepositoryProvider, storageManagerProvider, debugLoggerProvider);
  }

  public static MainViewModel newInstance(Application application,
      PermissionManager permissionManager, RecordingRepository recordingRepository,
      StorageManager storageManager, DebugLogger debugLogger) {
    return new MainViewModel(application, permissionManager, recordingRepository, storageManager, debugLogger);
  }
}
