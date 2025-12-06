package com.voicelife.assistant.di;

import com.voicelife.assistant.data.database.AppDatabase;
import com.voicelife.assistant.data.database.RecordingDao;
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
public final class DatabaseModule_ProvideRecordingDaoFactory implements Factory<RecordingDao> {
  private final Provider<AppDatabase> databaseProvider;

  public DatabaseModule_ProvideRecordingDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public RecordingDao get() {
    return provideRecordingDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideRecordingDaoFactory create(
      Provider<AppDatabase> databaseProvider) {
    return new DatabaseModule_ProvideRecordingDaoFactory(databaseProvider);
  }

  public static RecordingDao provideRecordingDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideRecordingDao(database));
  }
}
