package com.voicelife.assistant.storage;

import android.content.Context;
import com.voicelife.assistant.data.repository.RecordingRepository;
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
public final class StorageManager_Factory implements Factory<StorageManager> {
  private final Provider<Context> contextProvider;

  private final Provider<RecordingRepository> recordingRepositoryProvider;

  public StorageManager_Factory(Provider<Context> contextProvider,
      Provider<RecordingRepository> recordingRepositoryProvider) {
    this.contextProvider = contextProvider;
    this.recordingRepositoryProvider = recordingRepositoryProvider;
  }

  @Override
  public StorageManager get() {
    return newInstance(contextProvider.get(), recordingRepositoryProvider.get());
  }

  public static StorageManager_Factory create(Provider<Context> contextProvider,
      Provider<RecordingRepository> recordingRepositoryProvider) {
    return new StorageManager_Factory(contextProvider, recordingRepositoryProvider);
  }

  public static StorageManager newInstance(Context context,
      RecordingRepository recordingRepository) {
    return new StorageManager(context, recordingRepository);
  }
}
