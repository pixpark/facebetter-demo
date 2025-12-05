package com.pixpark.fbexample;

import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.pixpark.facebetter.BeautyEffectEngine;
import com.pixpark.facebetter.BeautyParams;
import com.pixpark.facebetter.BeautyParams.*;
import com.pixpark.facebetter.ImageFrame;

/**
 * 使用 externalContext + 纹理输入的最小 Android 示例。
 *
 * 流程：
 * 1. 使用 GLVideoRenderer 并设置 onProcessVideoFrame 回调
 * 2. GLVideoRenderer 内部读取图片生成纹理，在回调中带出
 * 3. 在回调中使用 externalContext=true 创建 BeautyEffectEngine，并调用 processImage
 * 4. 将处理后的纹理返回给 GLVideoRenderer 进行渲染
 */
public class ExternalTextureActivity
    extends AppCompatActivity implements GLTextureRenderer.OnProcessVideoFrameCallback {
  private static final String TAG = "ExternalTextureActivity";
  private static final int ERROR_SUCCESS = 0;
  private static final int ERROR_CREATE_FRAME_FAILED = -1;
  private static final int ERROR_PROCESS_FAILED = -2;
  private static final int ERROR_GET_BUFFER_FAILED = -3;
  private static final int ERROR_CONFIG_INVALID = -4;

  private GLTextureRenderer glVideoRenderer;
  private BeautyEffectEngine engine;
  private SeekBar seekBarSmoothing;
  private SeekBar seekBarWhitening;
  private TextView textSmoothingValue;
  private TextView textWhiteningValue;
  private float initialSmoothingValue = 0.2f;
  private float initialWhiteningValue = 0.0f;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_external_texture);

    glVideoRenderer = findViewById(R.id.gl_video_renderer);
    glVideoRenderer.setOnProcessVideoFrameCallback(this);

    // 初始化滑动条
    seekBarSmoothing = findViewById(R.id.seekbar_smoothing);
    seekBarWhitening = findViewById(R.id.seekbar_whitening);
    textSmoothingValue = findViewById(R.id.text_smoothing_value);
    textWhiteningValue = findViewById(R.id.text_whitening_value);

    // 设置磨皮滑动条监听器
    seekBarSmoothing.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser && engine != null) {
          float value = progress / 100.0f;
          engine.setBeautyParam(BasicParam.SMOOTHING, value);
          textSmoothingValue.setText(String.format("%.2f", value));
          Log.d(TAG, "Set SMOOTHING: " + value);
        }
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {}

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {}
    });

    // 设置美白滑动条监听器
    seekBarWhitening.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser && engine != null) {
          float value = progress / 100.0f;
          engine.setBeautyParam(BasicParam.WHITENING, value);
          textWhiteningValue.setText(String.format("%.2f", value));
          Log.d(TAG, "Set WHITENING: " + value);
        }
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {}

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {}
    });

    // 初始化显示值
    initialSmoothingValue = seekBarSmoothing.getProgress() / 100.0f;
    initialWhiteningValue = seekBarWhitening.getProgress() / 100.0f;
    textSmoothingValue.setText(String.format("%.2f", initialSmoothingValue));
    textWhiteningValue.setText(String.format("%.2f", initialWhiteningValue));
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (glVideoRenderer != null) {
      glVideoRenderer.cleanup();
    }
    if (engine != null) {
      engine.release();
      engine = null;
    }
  }

  @Override
  public int onProcessVideoFrame(
      GLTextureRenderer.TextureFrame srcFrame, GLTextureRenderer.TextureFrame dstFrame) {
    // Initialize engine if not initialized
    if (engine == null) {
      BeautyEffectEngine.LogConfig logConfig = new BeautyEffectEngine.LogConfig();
      logConfig.consoleEnabled = true;
      logConfig.level = BeautyEffectEngine.LogLevel.INFO;
      BeautyEffectEngine.setLogConfig(logConfig);

      BeautyEffectEngine.EngineConfig config = new BeautyEffectEngine.EngineConfig();
      // TODO: 替换为你的 AppId/AppKey 或 licenseJson
      config.appId = "";
      config.appKey = "";
      
      // 验证 appId 和 appKey
      if (config.appId == null || config.appKey == null ||
          config.appId.trim().isEmpty() || config.appKey.trim().isEmpty()) {
        Log.e(TAG, "[Facebetter] Error: appId and appKey must be configured. Please set your appId and appKey in the code.");
        return ERROR_CONFIG_INVALID;
      }
      
      config.externalContext = true;

      engine = new BeautyEffectEngine(this, config);
      engine.enableBeautyType(BeautyParams.BeautyType.BASIC, true);
      engine.enableBeautyType(BeautyParams.BeautyType.RESHAPE, true);

      // 应用初始滑动条的值
      engine.setBeautyParam(BasicParam.SMOOTHING, initialSmoothingValue);
      engine.setBeautyParam(BasicParam.WHITENING, initialWhiteningValue);
      Log.d(TAG,
          "BeautyEffectEngine initialized with SMOOTHING: " + initialSmoothingValue
              + ", WHITENING: " + initialWhiteningValue);
    }

    // Create ImageFrame from input texture
    int stride = srcFrame.width * 4; // RGBA stride
    ImageFrame inputFrame =
        ImageFrame.createWithTexture(srcFrame.textureId, srcFrame.width, srcFrame.height, stride);
    if (inputFrame == null) {
      Log.e(TAG, "createWithTexture failed");
      return ERROR_CREATE_FRAME_FAILED;
    }

    // Process image
    inputFrame.type = ImageFrame.FrameType.IMAGE;
    ImageFrame outputFrame = engine.processImage(inputFrame);
    if (outputFrame == null) {
      Log.e(TAG, "processImage returned null");
      return ERROR_PROCESS_FAILED;
    }

    // Get texture ID and dimensions directly from output frame
    int textureId = outputFrame.getTexture();
    if (textureId == 0) {
      Log.e(TAG, "getTexture returned 0");
      return ERROR_GET_BUFFER_FAILED;
    }

    // Set output texture and size
    dstFrame.textureId = textureId;
    dstFrame.width = outputFrame.getWidth();
    dstFrame.height = outputFrame.getHeight();

    outputFrame.release();
    inputFrame.release();
    return ERROR_SUCCESS;
  }
}
