package com.voicelife.assistant.di;

import com.voicelife.assistant.data.database.AppDatabase;
import com.voicelife.assistant.data.database.TranscriptionDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideTranscriptionDaoFactory implements Factory<TranscriptionDao> {
  private final Provider<AppDatabase> databaseProvider;

  public DatabaseModule_ProvideTranscriptionDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public TranscriptionDao get() {
    return provideTranscriptionDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideTranscriptionDaoFactory create(
      Provider<AppDatabase> databaseProvider) {
    return new DatabaseModule_ProvideTranscriptionDaoFactory(databaseProvider);
  }

  public static TranscriptionDao provideTranscriptionDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideTranscriptionDao(database));
  }
}
