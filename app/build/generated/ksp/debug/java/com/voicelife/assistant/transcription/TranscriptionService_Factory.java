package com.voicelife.assistant.transcription;

import android.content.Context;
import com.voicelife.assistant.data.repository.RecordingRepository;
import com.voicelife.assistant.utils.DebugLogger;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class TranscriptionService_Factory implements Factory<TranscriptionService> {
  private final Provider<Context> contextProvider;

  private final Provider<RecordingRepository> recordingRepositoryProvider;

  private final Provider<DebugLogger> debugLoggerProvider;

  public TranscriptionService_Factory(Provider<Context> contextProvider,
      Provider<RecordingRepository> recordingRepositoryProvider,
      Provider<DebugLogger> debugLoggerProvider) {
    this.contextProvider = contextProvider;
    this.recordingRepositoryProvider = recordingRepositoryProvider;
    this.debugLoggerProvider = debugLoggerProvider;
  }

  @Override
  public TranscriptionService get() {
    return newInstance(contextProvider.get(), recordingRepositoryProvider.get(), debugLoggerProvider.get());
  }

  public static TranscriptionService_Factory create(Provider<Context> contextProvider,
      Provider<RecordingRepository> recordingRepositoryProvider,
      Provider<DebugLogger> debugLoggerProvider) {
    return new TranscriptionService_Factory(contextProvider, recordingRepositoryProvider, debugLoggerProvider);
  }

  public static TranscriptionService newInstance(Context context,
      RecordingRepository recordingRepository, DebugLogger debugLogger) {
    return new TranscriptionService(context, recordingRepository, debugLogger);
  }
}
