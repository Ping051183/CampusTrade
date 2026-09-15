/**
 * 图片压缩工具
 * 在上传前使用 Canvas API 对图片进行压缩，减少带宽和存储
 */

interface CompressOptions {
  /** 最大宽度（默认 1920） */
  maxWidth?: number
  /** 最大高度（默认 1920） */
  maxHeight?: number
  /** JPEG/WebP 压缩质量 0-1（默认 0.85） */
  quality?: number
  /** 触发压缩的文件大小阈值（字节），超过此大小才压缩（默认 512KB） */
  sizeThreshold?: number
  /** 输出格式（默认自动选择） */
  outputType?: 'image/jpeg' | 'image/png' | 'image/webp'
}

const DEFAULT_OPTIONS: Required<CompressOptions> = {
  maxWidth: 1920,
  maxHeight: 1920,
  quality: 0.85,
  sizeThreshold: 512 * 1024, // 512KB
  outputType: 'image/jpeg'
}

/**
 * 压缩图片文件
 *
 * - 仅当文件超过 sizeThreshold 或图片尺寸超过 maxWidth/maxHeight 时压缩
 * - PNG 保持 PNG 格式（保留透明通道）
 * - GIF 跳过压缩（保留动画）
 * - 如果压缩后反而变大，返回原文件
 *
 * @param file 原始图片文件
 * @param options 压缩选项
 * @returns 压缩后的 File 对象
 */
export async function compressImage(file: File, options: CompressOptions = {}): Promise<File> {
  const opts = { ...DEFAULT_OPTIONS, ...options }

  // GIF 动画不压缩
  if (file.type === 'image/gif') {
    return file
  }

  // 小文件无需处理（减少不必要的 FileReader/Canvas 开销）
  if (file.size <= opts.sizeThreshold) {
    return file
  }

  return new Promise((resolve) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        // 计算缩放尺寸
        let width = img.naturalWidth
        let height = img.naturalHeight
        let needsResize = false

        if (width > opts.maxWidth) {
          height = height * (opts.maxWidth / width)
          width = opts.maxWidth
          needsResize = true
        }
        if (height > opts.maxHeight) {
          width = width * (opts.maxHeight / height)
          height = opts.maxHeight
          needsResize = true
        }

        // 不需要缩放，返回原文件
        if (!needsResize) {
          resolve(file)
          return
        }

        const canvas = document.createElement('canvas')
        const ctx = canvas.getContext('2d')
        if (!ctx) {
          resolve(file)
          return
        }

        canvas.width = Math.round(width)
        canvas.height = Math.round(height)

        // 确定输出格式
        // PNG 保留原格式（避免透明通道丢失）
        const mimeType = file.type === 'image/png' ? 'image/png' : opts.outputType

        // JPEG/WebP 使用白色背景填充（防止透明区域变黑）
        if (mimeType !== 'image/png') {
          ctx.fillStyle = '#ffffff'
          ctx.fillRect(0, 0, canvas.width, canvas.height)
        }

        // 高质量缩放绘制
        ctx.imageSmoothingEnabled = true
        ctx.imageSmoothingQuality = 'high'
        ctx.drawImage(img, 0, 0, canvas.width, canvas.height)

        canvas.toBlob(
          (blob) => {
            if (!blob || blob.size >= file.size) {
              // 压缩后反而更大，返回原文件
              resolve(file)
              return
            }

            const extMap: Record<string, string> = {
              'image/jpeg': '.jpg',
              'image/png': '.png',
              'image/webp': '.webp'
            }
            const ext = extMap[mimeType] || '.jpg'
            const fileName = file.name.replace(/\.[^.]+$/, '') + ext

            const compressedFile = new File([blob], fileName, {
              type: mimeType,
              lastModified: Date.now()
            })

            console.log(
              `[图片压缩] ${file.name}: ${(file.size / 1024).toFixed(1)}KB → ${(compressedFile.size / 1024).toFixed(1)}KB ` +
              `(${Math.round(width)}x${Math.round(height)}, ${Math.round((1 - compressedFile.size / file.size) * 100)}% 节省)`
            )

            resolve(compressedFile)
          },
          mimeType,
          opts.quality
        )
      }
      img.onerror = () => resolve(file)
      img.src = e.target?.result as string
    }
    reader.onerror = () => resolve(file)
    reader.readAsDataURL(file)
  })
}
