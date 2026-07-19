package com.jotape.inkshelf.data.scanner

import com.jotape.inkshelf.data.db.entity.FileEntity
import com.jotape.inkshelf.data.db.entity.FolderEntity
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ScanResult(
    val folders: List<FolderEntity>,
    val files: List<FileEntity>,
)

/**
 * Varredura da biblioteca no sistema de arquivos.
 *
 * Nota de porte: no Android cada diretório era uma query ao `DocumentsProvider` do SAF, cara o
 * bastante para justificar quatro corrotinas em paralelo com semáforo. Aqui `listFiles()` é uma
 * syscall direta — a travessia serial numa thread de IO é rápida e muito mais simples de seguir.
 * Se um dia a varredura de unidade de rede ficar lenta, é aqui que o paralelismo volta.
 *
 * Convenções mantidas do app mobile:
 * - o ID de pasta/arquivo identifica o item no disco. No mobile era o document ID do SAF em
 *   Base64; aqui é o **caminho canônico**, que já é único e estável;
 * - a raiz escolhida pelo usuário não vira linha no banco: seus filhos diretos usam o
 *   `parentId` literal `"root"`. Isso permite várias raízes convivendo numa biblioteca só;
 * - pastas sem nenhum arquivo suportado na sua árvore são descartadas do resultado.
 */
class LibraryScanner {

    private val supportedExtensions = setOf("cbz", "cbr", "pdf", "zip", "rar", "epub")

    suspend fun scan(root: File): ScanResult = withContext(Dispatchers.IO) {
        val folders = mutableListOf<FolderEntity>()
        val files = mutableListOf<FileEntity>()

        // Junções e links simbólicos do Windows podem apontar para um ancestral e criar um ciclo
        // infinito. Guardar os caminhos canônicos já visitados corta o ciclo na segunda visita.
        val visited = HashSet<String>()

        // Pilha explícita em vez de recursão: uma biblioteca com hierarquia muito profunda
        // estouraria a pilha da JVM.
        val pending = ArrayDeque<Pair<File, String>>()
        pending.addLast(root to ROOT_ID)

        while (pending.isNotEmpty()) {
            val (dir, parentId) = pending.removeFirst()
            if (!visited.add(dir.canonicalPathOrNull() ?: continue)) continue

            // Diretório ilegível (permissão negada, unidade removida): ignora e segue.
            val children = dir.listFiles() ?: continue

            for (child in children) {
                if (child.isDirectory) {
                    val id = child.canonicalPathOrNull() ?: continue
                    folders += FolderEntity(
                        id = id,
                        parentId = parentId,
                        name = child.name,
                        lastModifiedAt = child.lastModified(),
                    )
                    pending.addLast(child to id)
                } else if (isSupported(child.name)) {
                    val id = child.canonicalPathOrNull() ?: continue
                    files += FileEntity(
                        id = id,
                        folderId = parentId,
                        name = child.name,
                    )
                }
            }
        }

        val folderIdsWithFiles = collectFolderIdsWithFiles(folders, files)
        ScanResult(folders.filter { it.id in folderIdsWithFiles }, files)
    }

    /**
     * IDs de todas as pastas com pelo menos um arquivo na sua árvore. Sobe da pasta de cada
     * arquivo até a raiz marcando os ancestrais, parando quando encontra um já marcado —
     * O(arquivos + pastas).
     */
    private fun collectFolderIdsWithFiles(
        folders: List<FolderEntity>,
        files: List<FileEntity>,
    ): Set<String> {
        val parentById = HashMap<String, String?>(folders.size)
        folders.forEach { parentById[it.id] = it.parentId }

        val result = HashSet<String>()
        files.forEach { file ->
            var current: String? = file.folderId
            while (current != null && current != ROOT_ID && parentById.containsKey(current)) {
                if (!result.add(current)) break
                current = parentById[current]
            }
        }
        return result
    }

    private fun isSupported(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in supportedExtensions

    companion object {
        /** Pai virtual dos itens no topo de cada raiz. Não existe como linha em `folders`. */
        const val ROOT_ID = "root"
    }
}

/**
 * Caminho canônico, que é o ID do item. Canônico e não absoluto de propósito: resolve `..`,
 * links e diferenças de caixa, então a mesma pasta escaneada por dois caminhos diferentes
 * produz o mesmo ID em vez de duplicar a biblioteca.
 */
private fun File.canonicalPathOrNull(): String? =
    try {
        canonicalPath
    } catch (_: Exception) {
        null
    }
