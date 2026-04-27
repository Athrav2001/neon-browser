package com.neo.downloader.exception

import com.neo.downloader.part.DownloadPart

class PartTooManyErrorException(
    part: DownloadPart,
    override val cause: Throwable
) : Exception(
        "this part $part have too many errors",
    cause,
)
