# Compressor

Picture compression for android. 

支持精确设置压缩分辨率，支持自动旋转照片角度(8种角度)

## 使用方式

```kotlin
// compress 是耗时操作，需要异步
coroutineScope.launch {
    val result = Comporessor.compress(
        context = context,
        file = File("xxx.jpg"),
        maxResolution = 1080,
        quality = 70,
    )
}
```

maxResolution 是压缩后图片的最大分辨率（长边不超过 maxResolution)。 且maxResolution 是精确匹配的，假设原图分
辨率是 4000 x 3000， maxResolution 是 1920，那么图片的分辨率会被精确的压缩到 1920 x 1440。

## Demo 演示说明

![screenshot](https://github.com/aitsuki/Compressor/assets/14817735/5e67fd68-73e0-4292-a043-618938241d64)

1. 标题栏右上角选择需要压缩的图片；
2. 屏幕上半部分显示的时原图，下半部分显示的是压缩后的图片；
3. 底部是控制压缩参数，拖动进度条调整参数，参数变更时会实时响应。
