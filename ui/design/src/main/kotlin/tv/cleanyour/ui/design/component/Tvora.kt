package tv.cleanyour.ui.design.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.cleanyour.ui.design.R
import tv.cleanyour.ui.design.icon.CytIcons
import tv.cleanyour.ui.design.theme.CytTheme

@Composable
fun TvoraWordmark(modifier: Modifier = Modifier) {
    val c = CytTheme.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(Modifier.size(32.dp)) {
            drawRoundRect(c.primary, cornerRadius = CornerRadius(size.width * .24f))
            val v = Path().apply { moveTo(size.width * .24f, size.height * .32f); lineTo(size.width * .5f, size.height * .7f); lineTo(size.width * .77f, size.height * .28f) }
            drawPath(v, c.onPrimary, style = Stroke(size.width * .09f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
        Text(stringResource(R.string.tvora_name), color = c.textPrimary,
            style = CytTheme.type.titleLarge.copy(fontSize = 25.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.8).sp))
    }
}

/** Original vector artwork: a personal home screen and its phone controller. */
@Composable
fun TvoraHero(modifier: Modifier = Modifier) {
    val c = CytTheme.colors
    val description = stringResource(R.string.tvora_hero_description)
    Canvas(modifier.fillMaxWidth().height(176.dp).semantics { contentDescription = description }) {
        val w = size.width; val h = size.height
        val frame = Offset(w * .025f, h * .10f); val screen = Size(w * .83f, h * .68f)
        drawRoundRect(c.surface2, frame, screen, CornerRadius(18.dp.toPx()))
        drawRoundRect(c.outlineStrong, frame, screen, CornerRadius(18.dp.toPx()), style = Stroke(1.dp.toPx()))
        drawRoundRect(c.bg, Offset(w * .065f, h * .17f), Size(w * .75f, h * .49f), CornerRadius(10.dp.toPx()))
        drawRoundRect(c.primary, Offset(w * .105f, h * .235f), Size(w * .25f, h * .035f), CornerRadius(2.dp.toPx()))
        drawRoundRect(c.textTertiary.copy(alpha = .5f), Offset(w * .105f, h * .30f), Size(w * .36f, h * .018f), CornerRadius(2.dp.toPx()))
        listOf(c.primary, c.accent, Color(0xFFF1C281), c.surface3).forEachIndexed { i, color ->
            val x = w * (.105f + .163f * i)
            drawRoundRect(color, Offset(x, h * .405f), Size(w * .135f, h * .16f), CornerRadius(7.dp.toPx()))
            drawCircle(c.bg.copy(alpha = .6f), 3.dp.toPx(), Offset(x + w * .0675f, h * .485f))
        }
        drawLine(c.outlineStrong, Offset(w * .33f, h * .82f), Offset(w * .55f, h * .82f), 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        val phone = Offset(w * .70f, h * .34f); val phoneSize = Size(w * .255f, h * .62f)
        drawRoundRect(c.primary, phone, phoneSize, CornerRadius(16.dp.toPx()))
        drawRoundRect(c.onPrimary, Offset(w * .718f, h * .38f), Size(w * .219f, h * .50f), CornerRadius(11.dp.toPx()))
        drawCircle(c.primary, w * .048f, Offset(w * .827f, h * .62f))
        drawCircle(c.onPrimary, w * .024f, Offset(w * .827f, h * .62f))
        drawRoundRect(c.primary, Offset(w * .78f, h * .76f), Size(w * .095f, h * .02f), CornerRadius(2.dp.toPx()))
        drawCircle(c.accent, 4.dp.toPx(), Offset(w * .92f, h * .17f))
        drawCircle(c.primary.copy(alpha = .35f), 2.dp.toPx(), Offset(w * .96f, h * .24f))
    }
}

@Composable
fun TvoraSetupSteps(modifier: Modifier = Modifier) {
    val labels = listOf(R.string.tvora_step_connect, R.string.tvora_step_choose, R.string.tvora_step_enjoy)
    val c = CytTheme.colors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { i, label ->
            Column(Modifier.weight(1f).background(c.surface1, RoundedCornerShape(14.dp)).padding(12.dp)) {
                Text("0${i + 1}", color = c.primary, style = CytTheme.type.mono.copy(fontWeight = FontWeight.Bold))
                Text(stringResource(label), color = c.textSecondary, style = CytTheme.type.bodySmall, modifier = Modifier.padding(top = 5.dp))
            }
        }
    }
}

@androidx.compose.runtime.Immutable
data class TvoraAction(val title: String, val detail: String?, val icon: ImageVector, val onClick: (() -> Unit)?)

@Composable
fun TvoraActionGrid(actions: List<TvoraAction>, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = if (maxWidth < 280.dp || LocalDensity.current.fontScale >= 1.4f) 1 else 2
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            actions.chunked(columns).forEach { row ->
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { action -> TvoraActionCard(action, Modifier.weight(1f).fillMaxHeight()) }
                    if (row.size < columns) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun TvoraActionCard(action: TvoraAction, modifier: Modifier = Modifier) {
    val c = CytTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Column(modifier.clip(shape).background(c.surface1).border(1.dp, c.outline, shape)
        .then(action.onClick?.let { Modifier.clickable(role = Role.Button, onClick = it) } ?: Modifier)
        .heightIn(min = 140.dp).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).background(c.primary.copy(alpha = .12f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                CytIcon(action.icon, null, tint = c.primary, size = 20.dp)
            }
            if (action.onClick != null) CytIcon(CytIcons.ChevronForward, null, tint = c.textTertiary, size = 16.dp)
        }
        Text(action.title, color = if (action.onClick == null) c.textTertiary else c.textPrimary,
            style = CytTheme.type.titleMedium, modifier = Modifier.padding(top = 16.dp))
        action.detail?.let { Text(it, color = c.textSecondary, style = CytTheme.type.bodySmall, modifier = Modifier.padding(top = 5.dp)) }
    }
}

@Composable
fun TvoraDeviceCard(name: String, status: String, level: String, thumbnail: ImageBitmap?, description: String,
    onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    val c = CytTheme.colors
    val shape = RoundedCornerShape(24.dp)
    Column(modifier.fillMaxWidth().clip(shape).background(c.surface2).border(1.dp, c.outlineStrong.copy(alpha = .45f), shape)
        .clickable(role = Role.Button, onClick = onRefresh).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CytIcon(CytIcons.Tv, null, tint = c.primary, size = 32.dp)
            Column(Modifier.weight(1f)) {
                Text(name, style = CytTheme.type.titleLarge, color = c.textPrimary)
                Text(level, style = CytTheme.type.bodySmall, color = c.primary, modifier = Modifier.padding(top = 3.dp))
            }
            CytIcon(CytIcons.Refresh, stringResource(R.string.tvora_refresh_preview), tint = c.textSecondary, size = 20.dp)
        }
        Text(status, color = c.textSecondary, style = CytTheme.type.bodySmall, modifier = Modifier.padding(top = 14.dp))
        if (thumbnail != null) Image(thumbnail, description, Modifier.padding(top = 16.dp).fillMaxWidth().aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
    }
}
