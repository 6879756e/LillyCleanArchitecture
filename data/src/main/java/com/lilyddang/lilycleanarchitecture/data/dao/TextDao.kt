package com.lilyddang.lilycleanarchitecture.data.dao

import androidx.room.*
import com.lilyddang.lilycleanarchitecture.data.model.TextEntity
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
interface TextDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTexts(texts: TextEntity): Single<Long>

    @Query("SELECT * FROM text ORDER BY id DESC")
    fun getAllTexts(): Flowable<List<TextEntity>>

    @Query("SELECT * FROM text WHERE content LIKE '%' || :content || '%' ORDER BY id DESC")
    fun getTextsByContent(content: String): Single<List<TextEntity>>

    @Delete
    fun delete(texts: TextEntity): Completable

    @Query("DELETE FROM text")
    fun deleteAllTexts(): Completable
}