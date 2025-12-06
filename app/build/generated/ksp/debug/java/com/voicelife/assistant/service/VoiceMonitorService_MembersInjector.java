package com.voicelife.assistant.service;

import com.voicelife.assistant.data.repository.RecordingRepository;
import com.voicelife.assistant.storage.StorageManager;
import com.voicelife.assistant.utils.DebugLogger;
import com.voicelife.assistant.utils.NotificationHelper;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class VoiceMonitorService_MembersInjector implements MembersInjector<VoiceMonitorService> {
  private final Provider<NotificationHelper> notificationHelperProvider;

  private final Provider<RecordingRepository> recordingRepositoryProvider;

  private final Provider<StorageManager> storageManagerProvider;

  private final Provider<DebugLogger> debugLoggerProvider;

  public VoiceMonitorService_MembersInjector(
      Provider<NotificationHelper> notificationHelperProvider,
      Provider<RecordingRepository> recordingRepositoryProvider,
      Provider<StorageManager> storageManagerProvider, Provider<DebugLogger> debugLoggerProvider) {
    this.notificationHelperProvider = notificationHelperProvider;
    this.recordingRepositoryProvider = recordingRepositoryProvider;
    this.storageManagerProvider = storageManagerProvider;
    this.debugLoggerProvider = debugLoggerProvider;
  }

  public static MembersInjector<VoiceMonitorService> create(
      Provider<NotificationHelper> notificationHelperProvider,
      Provider<RecordingRepository> recordingRepositoryProvider,
      Provider<StorageManager> storageManagerProvider, Provider<DebugLogger> debugLoggerProvider) {
    return new VoiceMonitorService_MembersInjector(notificationHelperProvider, recordingRepositoryProvider, storageManagerProvider, debugLoggerProvider);
  }

  @Override
  public void injectMembers(VoiceMonitorService instance) {
    injectNotificationHelper(instance, notificationHelperProvider.get());
    injectRecordingRepository(instance, recordingRepositoryProvider.get());
    injectStorageManager(instance, storageManagerProvider.get());
    injectDebugLogger(instance, debugLoggerProvider.get());
  }

  @InjectedFieldSignature("com.voicelife.assistant.service.VoiceMonitorService.notificationHelper")
  public static void injectNotificationHelper(VoiceMonitorService instance,
      NotificationHelper notificationHelper) {
    instance.notificationHelper = notificationHelper;
  }

  @InjectedFieldSignature("com.voicelife.assistant.service.VoiceMonitorService.recordingRepository")
  public static void injectRecordingRepository(VoiceMonitorService instance,
      RecordingRepository recordingRepository) {
    instance.recordingRepository = recordingRepository;
  }

  @InjectedFieldSignature("com.voicelife.assistant.service.VoiceMonitorService.storageManager")
  public static void injectStorageManager(VoiceMonitorService instance,
      StorageManager storageManager) {
    instance.storageManager = storageManager;
  }

  @InjectedFieldSignature("com.voicelife.assistant.service.VoiceMonitorService.debugLogger")
  public static void injectDebugLogger(VoiceMonitorService instance, DebugLogger debugLogger) {
    instance.debugLogger = debugLogger;
  }
}
