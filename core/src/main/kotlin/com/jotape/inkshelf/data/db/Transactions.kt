package com.jotape.inkshelf.data.db

import androidx.room.RoomDatabase
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection

/**
 * Executa [block] numa única transação de escrita.
 *
 * Nota de porte: no Android isso era `db.withTransaction { }`, que vem do `room-ktx` — artefato
 * que só existe para Android. Fora dele o equivalente é pegar a conexão de escrita e abrir uma
 * transação imediata; as chamadas de DAO dentro do bloco reaproveitam essa mesma conexão, então
 * o comportamento é o mesmo do mobile.
 *
 * "Imediata" significa que o SQLite trava para escrita já na abertura, em vez de esperar o
 * primeiro INSERT. É o que queremos aqui: os blocos abaixo sempre escrevem, e a trava adiada
 * poderia falhar no meio da transação se outra conexão chegasse primeiro.
 */
suspend fun <R> RoomDatabase.inkTransaction(block: suspend () -> R): R =
    useWriterConnection { transactor ->
        transactor.immediateTransaction { block() }
    }
