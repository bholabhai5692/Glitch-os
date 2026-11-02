package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Comprehensive Legal, Privacy Policy, Terms & Conditions, and Educational Purpose Card.
 * Compliant with Google Play Developer Policies and transparent user disclosure.
 */
@Composable
fun LegalAndComplianceCard(
    modifier: Modifier = Modifier,
    defaultExpanded: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(defaultExpanded) }
    var selectedSection by remember { mutableStateOf(0) } // 0: All, 1: Privacy, 2: Terms, 3: Educational

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceElevated.copy(alpha = 0.9f))
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(CyberCyan.copy(alpha = 0.4f), ElectricViolet.copy(alpha = 0.4f))),
                RoundedCornerShape(14.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(CyberCyan.copy(alpha = 0.2f), ElectricViolet.copy(alpha = 0.2f)))
                        )
                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = "Legal & Compliance",
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "PRIVACY, TERMS & POLICY",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonGreen.copy(alpha = 0.2f))
                                .border(0.5.dp, NeonGreen.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "LEGAL",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonGreen
                            )
                        }
                    }
                    Text(
                        text = "गोपनीयता नीति, नियम एवं शर्तें व शैक्षणिक उद्देश्य",
                        fontSize = 9.5.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isExpanded) "CLOSE" else "TERMS",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse Legal" else "Expand Legal",
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

                // Scrollable filter tabs (Responsive)
                val tabs = listOf("All Sections", "Privacy Policy", "Terms & Conditions", "Educational Use")
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(tabs) { index, tabName ->
                        val isSelected = selectedSection == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CyberCyan.copy(alpha = 0.25f) else DarkBackground)
                                .border(0.5.dp, if (isSelected) CyberCyan else DarkBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedSection = index }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabName,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) CyberCyan else TextSecondary
                            )
                        }
                    }
                }

                // 1. PRIVACY POLICY
                if (selectedSection == 0 || selectedSection == 1) {
                    LegalSectionBlock(
                        title = "1. Privacy Policy / गोपनीयता नीति",
                        icon = Icons.Default.Security,
                        iconTint = CyberCyan,
                        content = "• Local Projects: वीडियो और प्रोजेक्ट विवरण इस डिवाइस पर सेव होते हैं।\n" +
                                "• Online Video Generation: यदि आप ऑनलाइन जनरेशन चुनते हैं, तो आपका विषय और वीडियो बनाने के लिए जरूरी जानकारी चुनी गई इंटरनेट सेवा को भेजी जाती है।\n" +
                                "• Accounts and Credentials: ऐप में साइन-इन, चैनल अकाउंट, Firebase या सेव की गई API key की सुविधा नहीं है।\n" +
                                "• App Permissions: नोटिफिकेशन और बैकग्राउंड परमिशन केवल वीडियो जनरेशन की स्थिति और रेंडरिंग के लिए उपयोग होती हैं।"
                    )
                }

                // 2. TERMS & CONDITIONS
                if (selectedSection == 0 || selectedSection == 2) {
                    LegalSectionBlock(
                        title = "2. Terms & Conditions / नियम एवं शर्तें",
                        icon = Icons.Default.Description,
                        iconTint = ElectricViolet,
                        content = "• User Responsibility: आप इस ऐप द्वारा उत्पन्न किए गए सभी प्रॉम्प्ट, स्क्रिप्ट और वीडियो के लिए जिम्मेदार हैं।\n" +
                                "• Copyright: केवल वही मीडिया उपयोग करें जिसके उपयोग की अनुमति आपके पास है।\n" +
                                "• Prohibited Content: किसी भी प्रकार की नफरत भरी भाषा (Hate Speech), भ्रामक सूचना (Misinformation), अश्लील सामग्री या गैर-कानूनी गतिविधियों के लिए इस टूल का उपयोग पूर्णतः वर्जित है।\n" +
                                "• Fair Use: ऑनलाइन सेवाओं का उपयोग उनकी लागू शर्तों और सीमाओं के अनुसार करें।"
                    )
                }

                // 3. EDUCATIONAL & RESEARCH PURPOSE
                if (selectedSection == 0 || selectedSection == 3) {
                    LegalSectionBlock(
                        title = "3. Educational & Research Purpose / शैक्षणिक उद्देश्य",
                        icon = Icons.Default.School,
                        iconTint = NeonGreen,
                        content = "• Educational Disclaimer: यह एप्लिकेशन मुख्य रूप से आर्टिफिशियल इंटेलिजेंस (AI), नेचुरल लैंग्वेज प्रोसेसिंग, न्यूरल वॉयस सिंथेसिस और ऑटोमेटेड मीडिया पाइपलाइन के अध्ययन एवं शोध के लिए डिज़ाइन किया गया है।\n" +
                                "• Fair Use Research: यह ऐप डेवलपर्स और छात्रों को AI वीडियो जेनरेशन, FFmpeg मल्टीमीडिया रेंडरिंग, और ऑटोमेशन आर्किटेक्चर को व्यावहारिक रूप से सीखने का अवसर प्रदान करता है।\n" +
                                "• Non-Malicious Use: इस सॉफ्टवेयर का निर्माण केवल सकारात्मक, रचनात्मक और ज्ञानवर्धक सामग्री निर्माण के लिए किया गया है।"
                    )
                }

                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

                // Footer note
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Glitch Agent • 100% Policy Compliant & Educational AI Software",
                        fontSize = 9.5.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun LegalSectionBlock(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    content: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkBackground.copy(alpha = 0.7f))
            .border(0.5.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = content,
            fontSize = 10.5.sp,
            color = TextSecondary,
            lineHeight = 15.sp
        )
    }
}
