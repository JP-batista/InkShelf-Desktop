package com.jotape.inkshelf.data.db.entity

import androidx.room.Entity

/**
 * Marca que um arquivo da biblioteca foi classificado como pertencente a um personagem do
 * catálogo comic-metadata. Chave composta (fileId, characterId) — mesmo padrão de
 * [BookmarkEntity] — para que cada arquivo só tenha uma tag por personagem.
 *
 * [origin] distingue classificação automática (`AUTO`, feita pelo `CharacterMatcher`) de
 * correção manual do usuário (`MANUAL`) — uma tag `MANUAL` nunca deve ser sobrescrita por uma
 * reclassificação automática futura.
 */
@Entity(tableName = "file_character_tags", primaryKeys = ["fileId", "characterId"])
data class FileCharacterTagEntity(
    val fileId: String,
    val characterId: String,
    val origin: String,
    val createdAt: Long,
)
