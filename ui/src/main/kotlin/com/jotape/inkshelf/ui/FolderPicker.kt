package com.jotape.inkshelf.ui

import java.io.File
import javax.swing.JFileChooser
import javax.swing.UIManager

/**
 * Diálogo nativo de escolha de pasta.
 *
 * Nota de porte: substitui o Storage Access Framework do Android. Lá, escolher uma pasta também
 * concedia permissão persistente de leitura — aqui não existe esse conceito: o app já pode ler
 * qualquer coisa que o usuário do Windows possa ler, então basta o caminho.
 *
 * Precisa rodar na thread de eventos do Swing (o Compose Desktop roda sobre AWT/Swing), o que
 * quem chama garante ao invocar isto de dentro do `application`.
 */
object FolderPicker {

    fun chooseDirectory(title: String = "Escolha a pasta da sua biblioteca"): File? {
        // Sem isto o diálogo aparece com o visual Metal do Java, que destoa do Windows.
        runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) }

        val chooser = JFileChooser().apply {
            dialogTitle = title
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            isMultiSelectionEnabled = false
        }

        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile?.takeIf { it.isDirectory }
        } else {
            null
        }
    }
}
