# Migration Batch Checklist

Use this exact order; compile gate after each batch.

## Batch 1: `shared:utils` (39 files)
- Copy module source files into `app/src/main/kotlin` preserving package.
- Do not remap package names in this stage.
- Run compile gate (`:app:compileDebugKotlin`) and fix only batch-local breaks.

## Batch 2: `shared:config` (0 files)
- Copy module source files into `app/src/main/kotlin` preserving package.
- Do not remap package names in this stage.
- Run compile gate (`:app:compileDebugKotlin`) and fix only batch-local breaks.

## Batch 3: `shared:resources:contracts` (0 files)
- Copy module source files into `app/src/main/kotlin` preserving package.
- Do not remap package names in this stage.
- Run compile gate (`:app:compileDebugKotlin`) and fix only batch-local breaks.

## Batch 4: `shared:resources` (83 files)
- Copy module source files into `app/src/main/kotlin` preserving package.
- Do not remap package names in this stage.
- Run compile gate (`:app:compileDebugKotlin`) and fix only batch-local breaks.

## Batch 5: `shared:compose-utils` (20 files)
- Copy module source files into `app/src/main/kotlin` preserving package.
- Do not remap package names in this stage.
- Run compile gate (`:app:compileDebugKotlin`) and fix only batch-local breaks.

## Batch 6: `downloader:monitor` (8 files)
- Copy module source files into `app/src/main/kotlin` preserving package.
- Do not remap package names in this stage.
- Run compile gate (`:app:compileDebugKotlin`) and fix only batch-local breaks.

## Batch 7: `downloader:core` (97 files)
- Copy module source files into `app/src/main/kotlin` preserving package.
- Do not remap package names in this stage.
- Run compile gate (`:app:compileDebugKotlin`) and fix only batch-local breaks.

## Batch 8: `shared:updater` (12 files)
- Copy module source files into `app/src/main/kotlin` preserving package.
- Do not remap package names in this stage.
- Run compile gate (`:app:compileDebugKotlin`) and fix only batch-local breaks.

## Batch 9: `shared:auto-start` (4 files)
- Copy module source files into `app/src/main/kotlin` preserving package.
- Do not remap package names in this stage.
- Run compile gate (`:app:compileDebugKotlin`) and fix only batch-local breaks.

## Batch 10: `shared:app` (267 files)
- Copy module source files into `app/src/main/kotlin` preserving package.
- Do not remap package names in this stage.
- Run compile gate (`:app:compileDebugKotlin`) and fix only batch-local breaks.

