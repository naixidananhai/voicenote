package com.voicelife.assistant.data.repository;

import com.voicelife.assistant.data.database.RecordingDao;
import com.voicelife.assistant.data.database.TranscriptionDao;
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
public final class RecordingRepository_Factory implements Factory<RecordingRepository> {
  private final Provider<RecordingDao> recordingDaoProvider;

  private final Provider<TranscriptionDao> transcriptionDaoProvider;

  public RecordingRepository_Factory(Provider<RecordingDao> recordingDaoProvider,
      Provider<TranscriptionDao> transcriptionDaoProvider) {
    this.recordingDaoProvider = recordingDaoProvider;
    this.transcriptionDaoProvider = transcriptionDaoProvider;
  }

  @Override
  public RecordingRepository get() {
    return newInstance(recordingDaoProvider.get(), transcriptionDaoProvider.get());
  }

  public static RecordingRepository_Factory create(Provider<RecordingDao> recordingDaoProvider,
      Provider<TranscriptionDao> transcriptionDaoProvider) {
    return new RecordingRepository_Factory(recordingDaoProvider, transcriptionDaoProvider);
  }

  public static RecordingRepository newInstance(RecordingDao recordingDao,
      TranscriptionDao transcriptionDao) {
    return new RecordingRepository(recordingDao, transcriptionDao);
  }
}
