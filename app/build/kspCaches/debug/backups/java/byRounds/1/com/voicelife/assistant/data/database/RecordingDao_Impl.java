package com.voicelife.assistant.data.database;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.voicelife.assistant.data.model.Recording;
import com.voicelife.assistant.data.model.TranscriptionStatus;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class RecordingDao_Impl implements RecordingDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Recording> __insertionAdapterOfRecording;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<Recording> __deletionAdapterOfRecording;

  private final EntityDeletionOrUpdateAdapter<Recording> __updateAdapterOfRecording;

  private final SharedSQLiteStatement __preparedStmtOfDeleteExpired;

  public RecordingDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfRecording = new EntityInsertionAdapter<Recording>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `recordings` (`id`,`filePath`,`duration`,`fileSize`,`createdAt`,`transcriptionStatus`,`transcribedAt`,`deleteAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Recording entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getFilePath());
        statement.bindLong(3, entity.getDuration());
        statement.bindLong(4, entity.getFileSize());
        statement.bindLong(5, entity.getCreatedAt());
        final String _tmp = __converters.fromTranscriptionStatus(entity.getTranscriptionStatus());
        statement.bindString(6, _tmp);
        if (entity.getTranscribedAt() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getTranscribedAt());
        }
        statement.bindLong(8, entity.getDeleteAt());
      }
    };
    this.__deletionAdapterOfRecording = new EntityDeletionOrUpdateAdapter<Recording>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `recordings` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Recording entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfRecording = new EntityDeletionOrUpdateAdapter<Recording>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `recordings` SET `id` = ?,`filePath` = ?,`duration` = ?,`fileSize` = ?,`createdAt` = ?,`transcriptionStatus` = ?,`transcribedAt` = ?,`deleteAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Recording entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getFilePath());
        statement.bindLong(3, entity.getDuration());
        statement.bindLong(4, entity.getFileSize());
        statement.bindLong(5, entity.getCreatedAt());
        final String _tmp = __converters.fromTranscriptionStatus(entity.getTranscriptionStatus());
        statement.bindString(6, _tmp);
        if (entity.getTranscribedAt() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getTranscribedAt());
        }
        statement.bindLong(8, entity.getDeleteAt());
        statement.bindLong(9, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteExpired = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM recordings WHERE deleteAt < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final Recording recording, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfRecording.insertAndReturnId(recording);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final Recording recording, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfRecording.handle(recording);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final Recording recording, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfRecording.handle(recording);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteExpired(final long timestamp,
      final Continuation<? super Integer> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteExpired.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        try {
          __db.beginTransaction();
          try {
            final Integer _result = _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteExpired.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getById(final long id, final Continuation<? super Recording> $completion) {
    final String _sql = "SELECT * FROM recordings WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Recording>() {
      @Override
      @Nullable
      public Recording call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfFileSize = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSize");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfTranscriptionStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "transcriptionStatus");
          final int _cursorIndexOfTranscribedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "transcribedAt");
          final int _cursorIndexOfDeleteAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deleteAt");
          final Recording _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final int _tmpDuration;
            _tmpDuration = _cursor.getInt(_cursorIndexOfDuration);
            final long _tmpFileSize;
            _tmpFileSize = _cursor.getLong(_cursorIndexOfFileSize);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final TranscriptionStatus _tmpTranscriptionStatus;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfTranscriptionStatus);
            _tmpTranscriptionStatus = __converters.toTranscriptionStatus(_tmp);
            final Long _tmpTranscribedAt;
            if (_cursor.isNull(_cursorIndexOfTranscribedAt)) {
              _tmpTranscribedAt = null;
            } else {
              _tmpTranscribedAt = _cursor.getLong(_cursorIndexOfTranscribedAt);
            }
            final long _tmpDeleteAt;
            _tmpDeleteAt = _cursor.getLong(_cursorIndexOfDeleteAt);
            _result = new Recording(_tmpId,_tmpFilePath,_tmpDuration,_tmpFileSize,_tmpCreatedAt,_tmpTranscriptionStatus,_tmpTranscribedAt,_tmpDeleteAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Recording>> getAllFlow() {
    final String _sql = "SELECT * FROM recordings ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"recordings"}, new Callable<List<Recording>>() {
      @Override
      @NonNull
      public List<Recording> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfFileSize = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSize");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfTranscriptionStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "transcriptionStatus");
          final int _cursorIndexOfTranscribedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "transcribedAt");
          final int _cursorIndexOfDeleteAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deleteAt");
          final List<Recording> _result = new ArrayList<Recording>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Recording _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final int _tmpDuration;
            _tmpDuration = _cursor.getInt(_cursorIndexOfDuration);
            final long _tmpFileSize;
            _tmpFileSize = _cursor.getLong(_cursorIndexOfFileSize);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final TranscriptionStatus _tmpTranscriptionStatus;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfTranscriptionStatus);
            _tmpTranscriptionStatus = __converters.toTranscriptionStatus(_tmp);
            final Long _tmpTranscribedAt;
            if (_cursor.isNull(_cursorIndexOfTranscribedAt)) {
              _tmpTranscribedAt = null;
            } else {
              _tmpTranscribedAt = _cursor.getLong(_cursorIndexOfTranscribedAt);
            }
            final long _tmpDeleteAt;
            _tmpDeleteAt = _cursor.getLong(_cursorIndexOfDeleteAt);
            _item = new Recording(_tmpId,_tmpFilePath,_tmpDuration,_tmpFileSize,_tmpCreatedAt,_tmpTranscriptionStatus,_tmpTranscribedAt,_tmpDeleteAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getByStatus(final TranscriptionStatus status,
      final Continuation<? super List<Recording>> $completion) {
    final String _sql = "SELECT * FROM recordings WHERE transcriptionStatus = ? ORDER BY createdAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    final String _tmp = __converters.fromTranscriptionStatus(status);
    _statement.bindString(_argIndex, _tmp);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Recording>>() {
      @Override
      @NonNull
      public List<Recording> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfFileSize = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSize");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfTranscriptionStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "transcriptionStatus");
          final int _cursorIndexOfTranscribedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "transcribedAt");
          final int _cursorIndexOfDeleteAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deleteAt");
          final List<Recording> _result = new ArrayList<Recording>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Recording _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final int _tmpDuration;
            _tmpDuration = _cursor.getInt(_cursorIndexOfDuration);
            final long _tmpFileSize;
            _tmpFileSize = _cursor.getLong(_cursorIndexOfFileSize);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final TranscriptionStatus _tmpTranscriptionStatus;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfTranscriptionStatus);
            _tmpTranscriptionStatus = __converters.toTranscriptionStatus(_tmp_1);
            final Long _tmpTranscribedAt;
            if (_cursor.isNull(_cursorIndexOfTranscribedAt)) {
              _tmpTranscribedAt = null;
            } else {
              _tmpTranscribedAt = _cursor.getLong(_cursorIndexOfTranscribedAt);
            }
            final long _tmpDeleteAt;
            _tmpDeleteAt = _cursor.getLong(_cursorIndexOfDeleteAt);
            _item = new Recording(_tmpId,_tmpFilePath,_tmpDuration,_tmpFileSize,_tmpCreatedAt,_tmpTranscriptionStatus,_tmpTranscribedAt,_tmpDeleteAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getExpiredRecordings(final long timestamp,
      final Continuation<? super List<Recording>> $completion) {
    final String _sql = "SELECT * FROM recordings WHERE deleteAt < ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, timestamp);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Recording>>() {
      @Override
      @NonNull
      public List<Recording> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfFileSize = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSize");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfTranscriptionStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "transcriptionStatus");
          final int _cursorIndexOfTranscribedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "transcribedAt");
          final int _cursorIndexOfDeleteAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deleteAt");
          final List<Recording> _result = new ArrayList<Recording>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Recording _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final int _tmpDuration;
            _tmpDuration = _cursor.getInt(_cursorIndexOfDuration);
            final long _tmpFileSize;
            _tmpFileSize = _cursor.getLong(_cursorIndexOfFileSize);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final TranscriptionStatus _tmpTranscriptionStatus;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfTranscriptionStatus);
            _tmpTranscriptionStatus = __converters.toTranscriptionStatus(_tmp);
            final Long _tmpTranscribedAt;
            if (_cursor.isNull(_cursorIndexOfTranscribedAt)) {
              _tmpTranscribedAt = null;
            } else {
              _tmpTranscribedAt = _cursor.getLong(_cursorIndexOfTranscribedAt);
            }
            final long _tmpDeleteAt;
            _tmpDeleteAt = _cursor.getLong(_cursorIndexOfDeleteAt);
            _item = new Recording(_tmpId,_tmpFilePath,_tmpDuration,_tmpFileSize,_tmpCreatedAt,_tmpTranscriptionStatus,_tmpTranscribedAt,_tmpDeleteAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
