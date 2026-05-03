package com.example.sitekiver01.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sitekiver01.ui.theme.*
import java.util.*

@Composable
fun ModernFormCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = GlassSurface,
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(Modifier.padding(20.dp)) { content() }
    }
}

@Composable
fun ModernSectionHeader(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(bottom = 12.dp)) {
        Icon(icon, null, tint = GlassAccentCyan, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = text, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color.White, letterSpacing = 0.5.sp)
    }
}

@Composable
fun ModernClickableField(
    value: String,
    placeholder: String,
    color: Color = Color.White.copy(alpha = 0.05f),
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        border = BorderStroke(1.dp, GlassBorder),
        shape = RoundedCornerShape(16.dp),
        color = color
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (value.isEmpty()) placeholder else value,
                color = if (value.isEmpty()) GlassTextMuted else Color.White,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(Icons.Default.ArrowDropDown, null, tint = GlassTextMuted)
        }
    }
}

@Composable
fun FormLabel(text: String) {
    Text(text = text, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = GlassTextMuted, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
}

@Composable
fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.padding(end = 8.dp, bottom = 8.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) GlassAccentCyan else Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, if(selected) GlassAccentCyan else GlassBorder)
    ) {
        Text(text = text, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = if (selected) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ModernDatePickerField(value: String, placeholder: String = "", onDateSelected: (String) -> Unit) {
    val context = LocalContext.current
    Surface(
        onClick = {
            val cal = Calendar.getInstance()
            DatePickerDialog(context, { _, y, m, d -> onDateSelected(String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m + 1, y)) }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        border = BorderStroke(1.dp, GlassBorder),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarMonth, null, tint = GlassAccentCyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text(text = if(value.isEmpty()) placeholder else value, fontSize = 14.sp, color = if(value.isEmpty()) GlassTextMuted else Color.White)
        }
    }
}

@Composable
fun ModernTimePickerField(value: String, placeholder: String = "", onTimeSelected: (String) -> Unit) {
    val context = LocalContext.current
    Surface(
        onClick = {
            val cal = Calendar.getInstance()
            TimePickerDialog(context, { _, h, m -> onTimeSelected(String.format(Locale.getDefault(), "%02d:%02d", h, m)) }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        border = BorderStroke(1.dp, GlassBorder),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccessTime, null, tint = GlassAccentCyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text(text = if(value.isEmpty()) placeholder else value, fontSize = 14.sp, color = if(value.isEmpty()) GlassTextMuted else Color.White)
        }
    }
}

@Composable
fun ModernDropdownField(selected: String, options: List<String>, label: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.padding(vertical = 4.dp)) {
        ModernClickableField(selected, label) { expanded = true }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.85f).background(Color(0xFF1A1A1A)).border(1.dp, GlassBorder)
        ) {
            if (options.isEmpty()) {
                DropdownMenuItem(text = { Text("Pilih data dulu", color = GlassTextMuted) }, onClick = { expanded = false })
            } else {
                options.forEach { opt ->
                    DropdownMenuItem(text = { Text(opt, fontSize = 14.sp, color = Color.White) }, onClick = { onSelected(opt); expanded = false })
                }
            }
        }
    }
}

@Composable
fun ModernSearchField(value: String, placeholder: String, onClick: () -> Unit, isDropdown: Boolean = false) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        border = BorderStroke(1.dp, GlassBorder),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.05f)
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (value.isEmpty()) placeholder else value,
                color = if (value.isEmpty()) GlassTextMuted else Color.White,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isDropdown) Icon(Icons.Default.ArrowDropDown, null, tint = GlassTextMuted, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun ModernTextField(
    value: String,
    placeholder: String,
    icon: ImageVector? = null,
    minLines: Int = 1,
    onValueChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        border = BorderStroke(1.dp, GlassBorder),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, null, tint = GlassAccentCyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush = SolidColor(GlassAccentCyan),
                minLines = minLines,
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(placeholder, color = GlassTextMuted, fontSize = 14.sp)
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
fun ModernButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    containerColor: Color = GlassAccentCyan
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f)
        ),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(text = text, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = GlassBorder,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        content()
    }
}
