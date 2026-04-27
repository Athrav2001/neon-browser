package com.neo.downloader.shared.util.extractors

interface Extractor<in Input,out Output>{
    fun extract(input:Input):Output
}
