package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.GameSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

enum class SoundEffect {
  CLICK,
  SHOOT,
  BOUNCE,
  COLLISION,
  POP_0,
  POP_1,
  POP_2,
  POP_3,
  COMBO,
  BOMB,
  RAINBOW,
  LIGHTNING,
  FIRE,
  FREEZE,
  COIN,
  CHEST,
  UNLOCK,
  REWARD,
  VICTORY,
  GAME_OVER,
  TICK,
}

class SoundEngine(context: Context) {
  private val appContext = context.applicationContext
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val soundPool: SoundPool = SoundPool.Builder()
    .setMaxStreams(10)
    .setAudioAttributes(
      AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build(),
    )
    .build()

  private val loadedIds = mutableMapOf<SoundEffect, Int>()
  private var mediaPlayer: MediaPlayer? = null
  private var musicFile: File? = null

  @Volatile
  private var settings = GameSettings()

  @Volatile
  private var isForeground = false

  @Volatile
  private var isInitialized = false

  @Volatile
  private var isReleased = false

  fun initialize() {
    if (isInitialized || isReleased) return
    isInitialized = true
    scope.launch {
      try {
        val dir = File(appContext.cacheDir, "bb_audio_v1").apply { mkdirs() }
        generateAllSfx(dir)
        val mus = File(dir, "music_loop.wav")
        if (!mus.exists() || mus.length() < 1024L) {
          writeWav(mus, SAMPLE_RATE, synthesizeMusicLoop())
        }
        musicFile = mus
        if (!isReleased) {
          startOrSyncMusicOnMainSafe()
        }
      } catch (_: Throwable) {
        // Audio hardware/storage issues must never crash gameplay
      }
    }
  }

  fun updateSettings(newSettings: GameSettings) {
    settings = newSettings
    syncMusicState()
  }

  fun onResume() {
    isForeground = true
    if (!isInitialized) initialize()
    syncMusicState()
  }

  fun onPause() {
    isForeground = false
    try {
      if (mediaPlayer?.isPlaying == true) {
        mediaPlayer?.pause()
      }
    } catch (_: Throwable) {
    }
  }

  fun release() {
    isReleased = true
    isForeground = false
    try {
      mediaPlayer?.release()
      mediaPlayer = null
    } catch (_: Throwable) {
    }
    try {
      soundPool.release()
    } catch (_: Throwable) {
    }
  }

  fun play(effect: SoundEffect) {
    if (isReleased || !settings.sfxEnabled) return
    val id = loadedIds[effect] ?: return
    val vol = (settings.sfxVolume / 100f).coerceIn(0f, 1f)
    if (vol <= 0.001f) return
    try {
      soundPool.play(id, vol, vol, 1, 0, 1.0f)
    } catch (_: Throwable) {
    }
  }

  fun playPop(index: Int) {
    val effect = when (index.coerceIn(0, 3)) {
      0 -> SoundEffect.POP_0
      1 -> SoundEffect.POP_1
      2 -> SoundEffect.POP_2
      else -> SoundEffect.POP_3
    }
    play(effect)
  }

  fun vibrate(durationMs: Long) {
    if (!settings.vibrationEnabled || durationMs <= 0L) return
    try {
      val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val mgr = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        mgr?.defaultVibrator
      } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      }
      if (vibrator?.hasVibrator() == true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
          @Suppress("DEPRECATION")
          vibrator.vibrate(durationMs)
        }
      }
    } catch (_: Throwable) {
    }
  }

  private fun startOrSyncMusicOnMainSafe() {
    val file = musicFile ?: return
    if (isReleased || !file.exists()) return
    try {
      if (mediaPlayer == null) {
        mediaPlayer = MediaPlayer().apply {
          setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_GAME)
              .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
              .build(),
          )
          setDataSource(file.absolutePath)
          isLooping = true
          prepare()
        }
      }
      syncMusicState()
    } catch (_: Throwable) {
    }
  }

  private fun syncMusicState() {
    val mp = mediaPlayer ?: run {
      if (musicFile != null && !isReleased) {
        startOrSyncMusicOnMainSafe()
      }
      return
    }
    try {
      val vol = if (settings.musicEnabled) (settings.musicVolume / 100f) * 0.55f else 0f
      mp.setVolume(vol.coerceIn(0f, 1f), vol.coerceIn(0f, 1f))
      if (isForeground && settings.musicEnabled && settings.musicVolume > 0) {
        if (!mp.isPlaying) mp.start()
      } else if (mp.isPlaying) {
        mp.pause()
      }
    } catch (_: Throwable) {
    }
  }

  private fun generateAllSfx(dir: File) {
    val specs: Map<SoundEffect, () -> ShortArray> = mapOf(
      SoundEffect.CLICK to { mix(tone(560f, 740f, 0.06f, Wave.SINE, 0.25f)) },
      SoundEffect.SHOOT to {
        mix(
          tone(290f, 720f, 0.13f, Wave.TRIANGLE, 0.34f),
          tone(520f, 700f, 0.08f, Wave.SINE, 0.16f, delaySec = 0.02f),
        )
      },
      SoundEffect.BOUNCE to { mix(tone(740f, 620f, 0.05f, Wave.SINE, 0.18f)) },
      SoundEffect.COLLISION to {
        mix(
          tone(220f, 150f, 0.09f, Wave.SINE, 0.30f),
          tone(330f, 290f, 0.05f, Wave.TRIANGLE, 0.14f, delaySec = 0.01f),
        )
      },
      SoundEffect.POP_0 to { popWave(460f) },
      SoundEffect.POP_1 to { popWave(540f) },
      SoundEffect.POP_2 to { popWave(620f) },
      SoundEffect.POP_3 to { popWave(710f) },
      SoundEffect.COMBO to {
        arpeggio(listOf(523.25f, 659.25f, 783.99f, 1046.5f), 0.18f, 0.055f, Wave.TRIANGLE, 0.24f)
      },
      SoundEffect.BOMB to {
        mix(
          tone(135f, 42f, 0.40f, Wave.SAW, 0.45f),
          tone(80f, 38f, 0.45f, Wave.TRIANGLE, 0.42f),
          noiseBurst(0.24f, 0.22f),
        )
      },
      SoundEffect.RAINBOW to {
        arpeggio(listOf(523.25f, 587.33f, 659.25f, 783.99f, 987.77f, 1046.5f), 0.20f, 0.045f, Wave.SINE, 0.22f)
      },
      SoundEffect.LIGHTNING to {
        mix(
          tone(980f, 240f, 0.28f, Wave.SAW, 0.28f),
          tone(1480f, 520f, 0.18f, Wave.SQUARE, 0.14f, delaySec = 0.03f),
        )
      },
      SoundEffect.FIRE to {
        mix(
          tone(240f, 520f, 0.30f, Wave.SAW, 0.28f),
          tone(420f, 240f, 0.24f, Wave.TRIANGLE, 0.22f, delaySec = 0.05f),
          noiseBurst(0.26f, 0.16f),
        )
      },
      SoundEffect.FREEZE to {
        arpeggio(listOf(1318.5f, 1567.98f, 2093.0f), 0.26f, 0.06f, Wave.SINE, 0.22f)
      },
      SoundEffect.COIN to {
        arpeggio(listOf(1174.7f, 1568.0f), 0.16f, 0.07f, Wave.TRIANGLE, 0.26f)
      },
      SoundEffect.CHEST to {
        arpeggio(listOf(392.0f, 493.88f, 587.33f, 783.99f, 987.77f), 0.24f, 0.07f, Wave.TRIANGLE, 0.26f)
      },
      SoundEffect.UNLOCK to {
        arpeggio(listOf(523.25f, 659.25f, 783.99f, 1046.5f, 1318.5f), 0.22f, 0.07f, Wave.SINE, 0.25f)
      },
      SoundEffect.REWARD to {
        arpeggio(listOf(587.33f, 739.99f, 880.0f, 1174.66f), 0.22f, 0.08f, Wave.TRIANGLE, 0.26f)
      },
      SoundEffect.VICTORY to {
        arpeggio(listOf(523.25f, 659.25f, 783.99f, 1046.5f, 1318.5f), 0.28f, 0.095f, Wave.TRIANGLE, 0.30f)
      },
      SoundEffect.GAME_OVER to {
        arpeggio(listOf(392.0f, 349.23f, 311.13f, 261.63f), 0.28f, 0.13f, Wave.TRIANGLE, 0.28f)
      },
      SoundEffect.TICK to { mix(tone(880f, 760f, 0.03f, Wave.SQUARE, 0.12f)) },
    )

    for ((effect, generator) in specs) {
      if (isReleased) return
      val file = File(dir, "${effect.name.lowercase()}.wav")
      if (!file.exists() || file.length() < 64L) {
        writeWav(file, SAMPLE_RATE, generator())
      }
      val soundId = soundPool.load(file.absolutePath, 1)
      loadedIds[effect] = soundId
    }
  }

  private fun popWave(baseFreq: Float): ShortArray = mix(
    tone(baseFreq, baseFreq + 320f, 0.11f, Wave.SINE, 0.34f),
    tone(baseFreq * 1.5f, baseFreq * 1.5f + 180f, 0.07f, Wave.TRIANGLE, 0.15f, delaySec = 0.01f),
  )

  private fun arpeggio(
    freqs: List<Float>,
    noteDurSec: Float,
    stepSec: Float,
    wave: Wave,
    amp: Float,
  ): ShortArray {
    val layers = freqs.mapIndexed { idx, freq ->
      tone(freq, freq, noteDurSec, wave, amp, delaySec = idx * stepSec)
    }
    return mix(*layers.toTypedArray())
  }

  private fun synthesizeMusicLoop(): ShortArray {
    // Seamless 4-bar relaxing magical celesta + warm bass progression (Cmaj7 -> Am9 -> Fmaj7 -> G6)
    val prog = listOf(
      130.81f to listOf(523.25f, 659.25f, 783.99f, 987.77f, 783.99f, 659.25f, 587.33f, 659.25f),
      110.00f to listOf(440.00f, 523.25f, 659.25f, 987.77f, 659.25f, 523.25f, 493.88f, 523.25f),
      87.31f to listOf(349.23f, 440.00f, 523.25f, 659.25f, 523.25f, 440.00f, 392.00f, 440.00f),
      98.00f to listOf(392.00f, 493.88f, 587.33f, 659.25f, 587.33f, 493.88f, 440.00f, 493.88f),
    )
    val stepSec = 0.32f
    val totalSteps = prog.size * 8
    val totalSamples = (totalSteps * stepSec * SAMPLE_RATE).roundToInt()
    val buf = FloatArray(totalSamples)

    for (step in 0 until totalSteps) {
      val bar = step / 8
      val beat = step % 8
      val (bassFreq, notes) = prog[bar]
      val startSample = (step * stepSec * SAMPLE_RATE).roundToInt()
      addSynthNote(buf, startSample, notes[beat], 0.42f, Wave.SINE, 0.14f)
      if (beat == 0 || beat == 4) {
        addSynthNote(buf, startSample, bassFreq, 0.72f, Wave.TRIANGLE, 0.12f)
      }
      if (beat == 2 || beat == 6) {
        addSynthNote(buf, startSample, notes[beat] * 0.5f, 0.35f, Wave.SINE, 0.07f)
      }
    }

    return ShortArray(totalSamples) { i ->
      val clamped = buf[i].coerceIn(-0.92f, 0.92f)
      (clamped * Short.MAX_VALUE).roundToInt().toShort()
    }
  }

  private fun addSynthNote(
    buf: FloatArray,
    startSample: Int,
    freq: Float,
    durSec: Float,
    wave: Wave,
    amp: Float,
  ) {
    val n = (durSec * SAMPLE_RATE).roundToInt().coerceAtLeast(1)
    val attack = (0.014f * SAMPLE_RATE).roundToInt().coerceAtLeast(1)
    for (i in 0 until n) {
      val t = i.toFloat() / SAMPLE_RATE
      val env = if (i < attack) i.toFloat() / attack else exp(-4.8f * (i - attack).toFloat() / n)
      val phase = (2.0 * PI * freq * t).toFloat()
      val sample = evalWave(wave, phase) * amp * env
      val idx = (startSample + i) % buf.size
      buf[idx] += sample
    }
  }

  private enum class Wave { SINE, TRIANGLE, SAW, SQUARE }

  private data class FloatLayer(val offsetSamples: Int, val samples: FloatArray)

  private fun tone(
    startFreq: Float,
    endFreq: Float,
    durSec: Float,
    wave: Wave,
    amp: Float,
    delaySec: Float = 0f,
  ): FloatLayer {
    val count = (durSec * SAMPLE_RATE).roundToInt().coerceAtLeast(1)
    val offset = (delaySec * SAMPLE_RATE).roundToInt().coerceAtLeast(0)
    val arr = FloatArray(count)
    var phase = 0.0
    val attack = (0.010f * SAMPLE_RATE).roundToInt().coerceAtLeast(1)
    for (i in 0 until count) {
      val p = i.toFloat() / count
      val freq = startFreq + (endFreq - startFreq) * p
      phase += 2.0 * PI * freq / SAMPLE_RATE
      val env = if (i < attack) i.toFloat() / attack else (1f - p).pow(1.6f)
      arr[i] = evalWave(wave, phase.toFloat()) * amp * env
    }
    return FloatLayer(offset, arr)
  }

  private fun noiseBurst(durSec: Float, amp: Float, delaySec: Float = 0f): FloatLayer {
    val count = (durSec * SAMPLE_RATE).roundToInt().coerceAtLeast(1)
    val offset = (delaySec * SAMPLE_RATE).roundToInt().coerceAtLeast(0)
    val arr = FloatArray(count)
    var seed = 0x1234567
    for (i in 0 until count) {
      seed = seed * 1664525 + 1013904223
      val rnd = ((seed shr 16) and 0xFFFF) / 32768f - 1f
      val env = 1f - (i.toFloat() / count)
      arr[i] = rnd * amp * env * env
    }
    return FloatLayer(offset, arr)
  }

  private fun mix(vararg layers: FloatLayer): ShortArray {
    var total = 1
    for (layer in layers) {
      total = max(total, layer.offsetSamples + layer.samples.size)
    }
    val acc = FloatArray(total)
    for (layer in layers) {
      for (i in layer.samples.indices) {
        acc[layer.offsetSamples + i] += layer.samples[i]
      }
    }
    return ShortArray(total) { i ->
      (acc[i].coerceIn(-0.95f, 0.95f) * Short.MAX_VALUE).roundToInt().toShort()
    }
  }

  private fun evalWave(wave: Wave, phase: Float): Float {
    val norm = ((phase / (2.0 * PI)) % 1.0 + 1.0) % 1.0
    return when (wave) {
      Wave.SINE -> sin(phase)
      Wave.TRIANGLE -> (1.0 - 4.0 * kotlin.math.abs(norm - 0.5)).toFloat()
      Wave.SAW -> (2.0 * norm - 1.0).toFloat()
      Wave.SQUARE -> if (norm < 0.5) 1f else -1f
    }
  }

  private fun writeWav(file: File, sampleRate: Int, pcm: ShortArray) {
    val dataSize = pcm.size * 2
    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
      put("RIFF".toByteArray())
      putInt(36 + dataSize)
      put("WAVE".toByteArray())
      put("fmt ".toByteArray())
      putInt(16) // PCM chunk size
      putShort(1) // AudioFormat = PCM
      putShort(1) // Mono
      putInt(sampleRate)
      putInt(sampleRate * 2)
      putShort(2) // Block align
      putShort(16) // Bits per sample
      put("data".toByteArray())
      putInt(dataSize)
    }.array()

    val body = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
    for (sample in pcm) body.putShort(sample)

    FileOutputStream(file).use { out ->
      out.write(header)
      out.write(body.array())
    }
  }

  companion object {
    private const val SAMPLE_RATE = 22050
  }
}
