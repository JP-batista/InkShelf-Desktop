package com.jotape.inkshelf.data.db.entity

import androidx.room.Entity

/**
 * Marca que um arquivo da biblioteca foi classificado como pertencente a uma equipe do catálogo
 * comic-metadata diretamente (nome do arquivo/pasta bateu no nome da equipe), independente de
 * qualquer personagem membro estar taggeado nesse mesmo arquivo. Mesmo padrão de
 * [FileCharacterTagEntity] (chave composta fileId+teamId, origin AUTO/MANUAL).
 */
@Entity(tableName = "file_team_tags", primaryKeys = ["fileId", "teamId"])
data class FileTeamTagEntity(
    val fileId: String,
    val teamId: String,
    val origin: String,
    val createdAt: Long,
)
