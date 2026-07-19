package com.jotape.inkshelf.data.scanner

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ScanState {
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    fun setScanning(value: Boolean) { _isScanning.value = value }
}