package io.github.vinaygaykar.mp1.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private object VinylConfig {
    /** Groove sheen tightness. Higher = narrower bow-tie. */
    const val SHARPNESS = 30f

    /** Sheen brightness on the black vinyl. */
    const val INTENSITY = 1.4f

    /** Label diameter as a fraction of the record diameter. */
    const val LABEL_FRACTION = 0.34f

    /** Spindle hole diameter as a fraction of the record diameter. */
    const val SPINDLE_FRACTION = 0.05f
}

/**
 * Standalone vinyl record: black disc with fine concentric grooves, glossy gaps between tracks,
 * and a warm-white anisotropic shine driven by [light]. Not used anywhere yet.
 *
 * Size it from outside (e.g. `Modifier.size(300.dp)`); it stays square.
 *
 * @param light world-fixed light in device space. Defaults to the live gyro + accelerometer light.
 * @param labelRotationDegrees spin angle applied to the label content only. The grooves are
 *  rotationally symmetric, so the shine must not rotate with the record. Drive this from your own animation.
 * @param label center label content. It gets the same concentric-line + shine treatment as artwork.
 * Below API 33 the record is a flat dark disc with the label on top.
 */
@Composable
fun VinylRecordGraphics(
    modifier: Modifier = Modifier,
    light: TiltLight = rememberTiltLight(active = true),
    labelRotationDegrees: Float = 0f,
    spindleColor: Color = Color(0xFF0C0B0A),
    label: @Composable BoxScope.() -> Unit = { DefaultVinylLabel() }
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .vinylSurface(light),
        contentAlignment = Alignment.Center
    ) {
        // Label: shine stays fixed to the screen, only the content rotates.
        Box(
            modifier = Modifier
                .fillMaxSize(VinylConfig.LABEL_FRACTION)
                .clip(CircleShape)
                .tiltShine(light, shineAlpha = 1f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = labelRotationDegrees },
                contentAlignment = Alignment.Center,
                content = label
            )
        }

        // Spindle hole
        Box(
            modifier = Modifier
                .fillMaxSize(VinylConfig.SPINDLE_FRACTION)
                .clip(CircleShape)
                .background(spindleColor)
        )
    }
}

@Composable
private fun BoxScope.DefaultVinylLabel() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C6B6B))
    )
}

@Composable
private fun Modifier.vinylSurface(light: TiltLight): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        vinylSurfaceAgsl(light)
    } else {
        drawBehind { drawRect(Color(0xFF111111)) }
    }

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun Modifier.vinylSurfaceAgsl(light: TiltLight): Modifier {
    val shader = remember { RuntimeShader(VINYL_AGSL) }
    val brush = remember(shader) { ShaderBrush(shader) }
    val dir = remember { FloatArray(3) }

    return drawBehind {
        light.direction(dir) // state read happens in draw -> redraw only, no recomposition
        shader.setFloatUniform("uRes", size.width, size.height)
        shader.setFloatUniform("uLight", dir[0], dir[1], dir[2])
        shader.setFloatUniform("uSharp", VinylConfig.SHARPNESS)
        shader.setFloatUniform("uGain", VinylConfig.INTENSITY)
        drawRect(brush = brush)
    }
}

/**
 * Vinyl in device space (N = V = +z). Regions by normalized radius r:
 *   r < 0.34        label area (left transparent, the label composable covers it)
 *   0.34 .. 0.37    run-in, 0.37 .. 0.95 grooved area, beyond that smooth rim
 * Grooved area: anisotropic sheen (Kajiya-Kay). Track gaps at r = 0.52 / 0.67 / 0.81 have no
 * grooves, so they reflect isotropically (thin glossy rings).
 */
private const val VINYL_AGSL = """
uniform float2 uRes;
uniform float3 uLight;
uniform float uSharp;
uniform float uGain;

half4 main(float2 fragCoord) {
    float2 uv = (fragCoord - 0.5 * uRes) / (0.5 * min(uRes.x, uRes.y));
    float r = length(uv);
    if (r > 1.0 || r < 0.34) return half4(0.0);

    float2 pd = float2(uv.x, -uv.y);                 // screen y points down, device y points up
    float2 tau = float2(-pd.y, pd.x) / max(r, 1e-4); // along the groove

    float3 L = normalize(uLight);
    float3 H = normalize(L + float3(0.0, 0.0, 1.0));
    float lf = smoothstep(0.0, 0.1, L.z);
    float diff = max(L.z, 0.0);

    float tH = dot(tau, H.xy);
    float sinTH = sqrt(max(1.0 - tH * tH, 0.0));
    float nh = max(H.z, 0.0);

    float grooved = smoothstep(0.37, 0.39, r) * (1.0 - smoothstep(0.95, 0.965, r));
    float gaps = 0.0;
    gaps += 1.0 - smoothstep(0.006, 0.012, abs(r - 0.52));
    gaps += 1.0 - smoothstep(0.006, 0.012, abs(r - 0.67));
    gaps += 1.0 - smoothstep(0.006, 0.012, abs(r - 0.81));
    gaps = min(gaps, 1.0);

    float groove = 0.85 + 0.15 * sin(r * 240.0);
    float broad = pow(sinTH, uSharp) * pow(nh, 6.0) * groove;
    float tight = pow(sinTH, uSharp * 6.0) * pow(nh, 2.0);

    float3 warm = float3(1.0, 0.95, 0.88);
    float3 irid = 0.5 + 0.5 * cos(6.2832 * (r * 3.0 + float3(0.0, 0.33, 0.67)));
    float3 tint = mix(warm, irid, 0.12);

    float3 an = (broad * 0.6 + tight * 0.7) * tint * grooved * (1.0 - 0.75 * gaps);
    float iso = pow(nh, 260.0) * (gaps * grooved + (1.0 - grooved)) * 0.9;

    float3 col = float3(0.016) + float3(0.025) * diff + uGain * lf * (an + iso * warm);
    col = col / (1.0 + col * 0.35);
    col = pow(col, float3(0.4545));
    return half4(half3(col), 1.0);
}
"""

@Preview(showBackground = true, backgroundColor = 0xFF202020)
@Composable
private fun VinylRecordGraphicsPreview() {
    Box(modifier = Modifier.size(320.dp), contentAlignment = Alignment.Center) {
        VinylRecordGraphics(modifier = Modifier.size(280.dp))
    }
}
