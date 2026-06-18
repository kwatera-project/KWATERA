import { useState } from "react";
import { format } from "date-fns";
import { jsPDF } from "jspdf";
import html2canvas from "html2canvas";
import { FileText, Download, Loader2 } from "lucide-react";
import { decodeJwt } from "../utils/jwtUtils";
import {useTranslation} from "react-i18next"
import { getLocaleCode } from "../utils/locale";

interface ReservationOverview {
  id: string;
  guestName: string;
  unitName: string;
  startDate: string;
  endDate: string;
  status: string;
  pricePerNightSnapshot?: number;
  totalPrice?: number;
}

interface ReservationMetrics {
  totalReservations: number;
  occupancyRate: number;
  occupiedDays: number;
}

interface BillingMetrics {
  revenueFromSettlements: number;
  unpaidBalance: number;
  paidSettlementsCount: number;
  unpaidSettlementsCount: number;
}

interface ReportExportButtonsProps {
  startDate: Date | null;
  endDate: Date | null;
  userRole: string;
  resMetrics: ReservationMetrics;
  billMetrics: BillingMetrics;
  totalUnitsCount: number;
  activeReservations: ReservationOverview[];
}

export default function ReportExportButtons({
  startDate,
  endDate,
  userRole,
  resMetrics,
  billMetrics,
  totalUnitsCount,
  activeReservations,
}: ReportExportButtonsProps) {
  const [isGenerating, setIsGenerating] = useState(false);
  const [statusMessage, setStatusMessage] = useState("");
  const {t, i18n} = useTranslation();
  const localeCode = getLocaleCode(i18n.language);

  const totalReservations = resMetrics?.totalReservations ?? 0;
  const occupancyRate = resMetrics?.occupancyRate ?? 0;
  const occupiedDays = resMetrics?.occupiedDays ?? 0;

  const revenueFromSettlements = billMetrics?.revenueFromSettlements ?? 0;
  const unpaidBalance = billMetrics?.unpaidBalance ?? 0;
  const paidSettlementsCount = billMetrics?.paidSettlementsCount ?? 0;
  const unpaidSettlementsCount = billMetrics?.unpaidSettlementsCount ?? 0;

  const token = localStorage.getItem("token");
  const payload = token ? decodeJwt(token) : null;
  const sub = payload?.sub as string | undefined;
  const firstName = payload?.firstName as string | undefined;
  const lastName = payload?.lastName as string | undefined;
  const displayName =
    firstName && lastName
      ? `${firstName} ${lastName}`
      : sub
      ? sub.split("@")[0]
      : t('report.userProfile');

  const formatDate = (date: Date | null) => {
    if (!date) return "N/A";
    return format(date, "yyyy-MM-dd");
  };

  const formatCurrency = (value: number) => {
    const rawString = value.toLocaleString(localeCode, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
    const cleanNumber = rawString.replace(/[\s\u00A0\u202F\u2007\u2008\u2009\u200A]/g, " ").trim();
    return `${cleanNumber} PLN`;
  };

  const handleDownloadCSV = () => {
    if (!startDate || !endDate) return;
    const startStr = formatDate(startDate);
    const endStr = formatDate(endDate);

    const csvRows = [
      [t('report.csvTitle')],
      [t('report.generatedOnValue', { value: new Date().toLocaleString(localeCode) })],
      [t('report.periodValue', { start: startStr, end: endStr })],
      [t('report.roleValue', { role: t(`report.roles.${userRole}`, { defaultValue: userRole }) })],
      [t('report.generatedByValue', { name: displayName })],
      [],
      [t('report.reservationMetrics')],
      [t('report.metric'), t('report.value')],
      [t('report.totalReservations'), String(totalReservations)],
      [t('report.occupancyRate'), `${occupancyRate}%`],
      [t('report.occupiedDays'), String(occupiedDays)],
      [t('report.totalScopedUnits'), String(totalUnitsCount)],
      [],
      [t('report.billingMetrics')],
      [t('report.metric'), t('report.valuePln')],
      [t('report.collectedRevenue'), String(revenueFromSettlements)],
      [t('report.unpaidBalance'), String(unpaidBalance)],
      [t('report.paidInvoicesCount'), String(paidSettlementsCount)],
      [t('report.unpaidInvoicesCount'), String(unpaidSettlementsCount)],
    ];

    const csvContent = csvRows
      .map((row) =>
        row.map((val) => (val.includes(",") ? `"${val}"` : val)).join(",")
      )
      .join("\n");

    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.setAttribute("href", url);
    link.setAttribute(
      "download",
      `kwatera-${userRole.toLowerCase()}-report-${startStr}-to-${endStr}.csv`
    );
    link.style.visibility = "hidden";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleDownloadPDF = async () => {
    if (!startDate || !endDate) return;
    setIsGenerating(true);
    setStatusMessage(t('report.loadingAssets'));

    try {
      const logoEl = await new Promise<HTMLImageElement | null>((resolve) => {
        const img = new Image();
        img.src = "/kwatera.png";
        img.onload = () => resolve(img);
        img.onerror = () => {
          resolve(null);
        };
      });

      setStatusMessage(t('report.capturingCharts'));
      await new Promise((resolve) => setTimeout(resolve, 300));

      const occupancyChartEl = document.getElementById("occupancy-chart");
      let occupancyImg = "";
      let occupancyAspect = 1.6;
      if (occupancyChartEl) {
        const canvas = await html2canvas(occupancyChartEl, {
          scale: 2,
          useCORS: true,
          backgroundColor: "#ffffff",
          logging: false,
        });
        occupancyImg = canvas.toDataURL("image/png");
        occupancyAspect = occupancyChartEl.offsetWidth / occupancyChartEl.offsetHeight;
      }

      const revenueChartEl = document.getElementById("revenue-chart");
      let revenueImg = "";
      let revenueAspect = 1.6;
      if (revenueChartEl) {
        const canvas = await html2canvas(revenueChartEl, {
          scale: 2,
          useCORS: true,
          backgroundColor: "#ffffff",
          logging: false,
        });
        revenueImg = canvas.toDataURL("image/png");
        revenueAspect = revenueChartEl.offsetWidth / revenueChartEl.offsetHeight;
      }

      setStatusMessage(t('report.generatingPdf'));

      const pdf = new jsPDF("p", "mm", "a4");
      // Report copy follows the selected language. jsPDF's built-in Helvetica is
      // retained to avoid adding a large font asset; full Polish glyph fidelity
      // depends on the PDF viewer until a Unicode font is embedded.

      if (logoEl) {
        pdf.addImage(logoEl, "PNG", 15, 14, 14, 14);
      }

      pdf.setFont("Helvetica", "bold");
      pdf.setFontSize(22);
      pdf.setTextColor(66, 33, 29);
      pdf.text("KWATERA", logoEl ? 32 : 15, 21);

      pdf.setFont("Helvetica", "bold");
      pdf.setFontSize(7.5);
      pdf.setTextColor(122, 122, 122);
      pdf.text(t('report.platform'), logoEl ? 32 : 15, 26);

      pdf.setFont("Helvetica", "bold");
      pdf.setFontSize(14);
      pdf.setTextColor(26, 26, 26);
      pdf.text(t('report.invoiceSummary'), 195, 21, { align: "right" });

      pdf.setFont("Helvetica", "normal");
      pdf.setFontSize(9);
      pdf.setTextColor(122, 122, 122);
      pdf.text(t('report.systemGeneratedSettlement'), 195, 26, { align: "right" });

      pdf.setDrawColor(66, 33, 29);
      pdf.setLineWidth(0.8);
      pdf.line(15, 32, 195, 32);

      pdf.setFont("Helvetica", "bold");
      pdf.setFontSize(9);
      pdf.setTextColor(66, 33, 29);
      pdf.text(t('report.issuer'), 15, 40);
      
      pdf.setFont("Helvetica", "bold");
      pdf.setFontSize(9);
      pdf.setTextColor(26, 26, 26);
      pdf.text("Kwatera Property Management Sp. z o.o.", 15, 45);
      pdf.setFont("Helvetica", "normal");
      pdf.setTextColor(85, 85, 85);
      pdf.text("ul. Widok 12, 00-023 Warszawa, Polska", 15, 50);
      pdf.text("NIP: 5252821944", 15, 55);
      pdf.text("Email: billing@kwatera.pl", 15, 60);

      pdf.setFont("Helvetica", "bold");
      pdf.setTextColor(66, 33, 29);
      pdf.text(t('report.client'), 110, 40);
      
      pdf.setFont("Helvetica", "bold");
      pdf.setTextColor(26, 26, 26);
      pdf.text(displayName, 110, 45);
      pdf.setFont("Helvetica", "normal");
      pdf.setTextColor(85, 85, 85);
      pdf.text(t('report.roleValue', { role: t(`report.roles.${userRole}`, { defaultValue: userRole }) }), 110, 50);
      pdf.text(t('report.emailValue', { email: sub || t('common.notAvailable') }), 110, 55);
      pdf.text(t('report.periodValue', { start: formatDate(startDate), end: formatDate(endDate) }), 110, 60);

      pdf.setFillColor(253, 253, 253);
      pdf.setDrawColor(218, 205, 202);
      pdf.setLineWidth(0.3);
      pdf.roundedRect(15, 66, 180, 18, 2, 2, "FD");

      pdf.setFont("Helvetica", "bold");
      pdf.setFontSize(8.5);
      pdf.setTextColor(66, 33, 29);
      pdf.text(t('report.documentType'), 20, 72);
      pdf.setFont("Helvetica", "normal");
      pdf.setTextColor(85, 85, 85);
      const docNo = `KWA/${new Date().getFullYear()}/${formatDate(endDate).replace(/-/g, "")}-${totalReservations}`;
      pdf.text(t('report.documentNumberValue', { number: docNo }), 20, 79);

      pdf.setFont("Helvetica", "normal");
      pdf.text(t('report.issueDateValue', { date: new Date().toLocaleDateString(localeCode) }), 115, 72);
      pdf.text(t('report.monitoredUnitsValue', { count: totalUnitsCount }), 115, 79);

      pdf.setFont("Helvetica", "bold");
      pdf.setFontSize(10);
      pdf.setTextColor(66, 33, 29);
      pdf.text(t('report.itemizedEntries'), 15, 92);

      pdf.setDrawColor(218, 205, 202);
      pdf.setLineWidth(0.4);
      pdf.line(15, 95, 195, 95);

      pdf.setFillColor(66, 33, 29);
      pdf.rect(15, 98, 180, 8, "F");

      pdf.setFont("Helvetica", "bold");
      pdf.setFontSize(8);
      pdf.setTextColor(255, 255, 255);
      pdf.text(t('report.numberShort'), 18.5, 103.5, { align: "center" });
      pdf.text(t('report.serviceDescription'), 24, 103.5);
      pdf.text(t('report.quantityShort'), 95, 103.5, { align: "center" });
      pdf.text(t('report.unitPriceNet'), 127.5, 103.5, { align: "right" });
      pdf.text(t('report.netValue'), 152.5, 103.5, { align: "right" });
      pdf.text("VAT", 162.5, 103.5, { align: "center" });
      pdf.text(t('report.grossValue'), 192.5, 103.5, { align: "right" });

      pdf.setDrawColor(218, 205, 202);
      pdf.setLineWidth(0.3);

      pdf.line(15, 106, 195, 106);
      pdf.line(15, 116, 195, 116);
      pdf.line(15, 126, 195, 126);

      pdf.line(15, 98, 15, 126);
      pdf.line(22, 98, 22, 126);
      pdf.line(85, 98, 85, 126);
      pdf.line(105, 98, 105, 126);
      pdf.line(130, 98, 130, 126);
      pdf.line(155, 98, 155, 126);
      pdf.line(170, 98, 170, 126);
      pdf.line(195, 98, 195, 126);

      const netAccommodation = revenueFromSettlements / 1.08;
      const vatAccommodation = revenueFromSettlements - netAccommodation;
      const unitAccommodation = occupiedDays > 0 ? netAccommodation / occupiedDays : 0;

      pdf.setFont("Helvetica", "normal");
      pdf.setFontSize(8);
      pdf.setTextColor(26, 26, 26);
      pdf.text("1", 18.5, 111, { align: "center" });
      const desc1 = t('report.accommodationService');
      const lines1 = pdf.splitTextToSize(desc1, 58);
      if (lines1.length === 1) {
        pdf.text(lines1[0], 24, 111);
      } else if (lines1.length >= 2) {
        pdf.text(lines1[0], 24, 110);
        pdf.text(lines1[1], 24, 113.5);
      }
      pdf.text(String(occupiedDays), 95, 111, { align: "center" });
      pdf.text(formatCurrency(unitAccommodation), 127.5, 111, { align: "right" });
      pdf.text(formatCurrency(netAccommodation), 152.5, 111, { align: "right" });
      pdf.text("8%", 162.5, 111, { align: "center" });
      pdf.text(formatCurrency(revenueFromSettlements), 192.5, 111, { align: "right" });

      const netReceivables = unpaidBalance / 1.08;
      const vatReceivables = unpaidBalance - netReceivables;

      pdf.text("2", 18.5, 121, { align: "center" });
      const desc2 = t('report.outstandingReceivables');
      const lines2 = pdf.splitTextToSize(desc2, 58);
      if (lines2.length === 1) {
        pdf.text(lines2[0], 24, 121);
      } else if (lines2.length >= 2) {
        pdf.text(lines2[0], 24, 120);
        pdf.text(lines2[1], 24, 123.5);
      }
      pdf.text("—", 95, 121, { align: "center" });
      pdf.text("—", 127.5, 121, { align: "right" });
      pdf.text(formatCurrency(netReceivables), 152.5, 121, { align: "right" });
      pdf.text("8%", 162.5, 121, { align: "center" });
      pdf.text(formatCurrency(unpaidBalance), 192.5, 121, { align: "right" });

      const totalNet = netAccommodation + netReceivables;
      const totalVat = vatAccommodation + vatReceivables;
      const totalGross = revenueFromSettlements + unpaidBalance;

      pdf.setFillColor(247, 247, 247);
      pdf.rect(130, 126, 65, 24, "F");
      
      pdf.line(130, 134, 195, 134);
      pdf.line(130, 142, 195, 142);
      pdf.line(130, 150, 195, 150);
      pdf.line(130, 126, 130, 150);
      pdf.line(160, 126, 160, 150);
      pdf.line(195, 126, 195, 150);

      pdf.setFont("Helvetica", "bold");
      pdf.setTextColor(66, 33, 29);
      pdf.text(t('report.netTotal'), 132, 131.5);
      pdf.setFont("Helvetica", "normal");
      pdf.setTextColor(26, 26, 26);
      pdf.text(formatCurrency(totalNet), 192.5, 131.5, { align: "right" });

      pdf.setFont("Helvetica", "bold");
      pdf.setTextColor(66, 33, 29);
      pdf.text(t('report.vatTotal'), 132, 139.5);
      pdf.setFont("Helvetica", "normal");
      pdf.setTextColor(26, 26, 26);
      pdf.text(formatCurrency(totalVat), 192.5, 139.5, { align: "right" });

      pdf.setFont("Helvetica", "bold");
      pdf.setTextColor(66, 33, 29);
      pdf.text(t('report.grossTotal'), 132, 147.5);
      pdf.setFont("Helvetica", "bold");
      pdf.setTextColor(66, 33, 29);
      pdf.text(formatCurrency(totalGross), 192.5, 147.5, { align: "right" });

      pdf.setFillColor(253, 253, 253);
      pdf.setDrawColor(218, 205, 202);
      pdf.roundedRect(15, 160, 110, 24, 2, 2, "FD");

      pdf.setFont("Helvetica", "bold");
      pdf.setFontSize(8);
      pdf.setTextColor(66, 33, 29);
      pdf.text(t('report.remarksStats'), 20, 165);
      pdf.setFont("Helvetica", "normal");
      pdf.setTextColor(85, 85, 85);
      pdf.text(t('report.bookingOrdersValue', { count: totalReservations }), 20, 171);
      pdf.text(t('report.settlementStatusValue', { paid: paidSettlementsCount, unpaid: unpaidSettlementsCount }), 20, 177);

      pdf.setDrawColor(218, 205, 202);
      pdf.line(140, 176, 190, 176);
      pdf.setFont("Helvetica", "normal");
      pdf.setFontSize(7.5);
      pdf.setTextColor(122, 122, 122);
      pdf.text(t('report.signature'), 165, 181, { align: "center" });

      pdf.addPage();

      if (logoEl) {
        pdf.addImage(logoEl, "PNG", 15, 14, 10, 10);
      }

      pdf.setFont("Helvetica", "bold");
      pdf.setFontSize(14);
      pdf.setTextColor(66, 33, 29);
      pdf.text("KWATERA", logoEl ? 28 : 15, 21);

      pdf.setFont("Helvetica", "bold");
      pdf.setFontSize(9);
      pdf.setTextColor(122, 122, 122);
      pdf.text(t('report.chartAnalytics'), 195, 21, { align: "right" });

      pdf.setDrawColor(218, 205, 202);
      pdf.setLineWidth(0.4);
      pdf.line(15, 28, 195, 28);

      pdf.setFont("Helvetica", "bold");
      pdf.setFontSize(11);
      pdf.setTextColor(66, 33, 29);
      pdf.text(t('report.occupancySnapshot'), 15, 36);

      pdf.setFillColor(255, 255, 255);
      pdf.setDrawColor(218, 205, 202);
      pdf.setLineWidth(0.3);
      pdf.roundedRect(15, 40, 180, 105, 3, 3, "FD");

      if (occupancyImg) {
        const cardW = 170;
        const cardH = 95;
        let w = cardW;
        let h = w / occupancyAspect;
        if (h > cardH) {
          h = cardH;
          w = h * occupancyAspect;
        }
        const x = 20 + (cardW - w) / 2;
        const y = 45 + (cardH - h) / 2;
        pdf.addImage(occupancyImg, "PNG", x, y, w, h);
      }

      pdf.setFont("Helvetica", "bold");
      pdf.setFontSize(11);
      pdf.setTextColor(66, 33, 29);
      pdf.text(t('report.revenueExpenseBreakdown'), 15, 156);

      pdf.setFillColor(255, 255, 255);
      pdf.setDrawColor(218, 205, 202);
      pdf.setLineWidth(0.3);
      pdf.roundedRect(15, 160, 180, 105, 3, 3, "FD");

      if (revenueImg) {
        const cardW = 170;
        const cardH = 95;
        let w = cardW;
        let h = w / revenueAspect;
        if (h > cardH) {
          h = cardH;
          w = h * revenueAspect;
        }
        const x = 20 + (cardW - w) / 2;
        const y = 165 + (cardH - h) / 2;
        pdf.addImage(revenueImg, "PNG", x, y, w, h);
      }

      pdf.addPage();

      if (logoEl) {
        pdf.addImage(logoEl, "PNG", 15, 14, 10, 10);
      }
      
      pdf.setFont("Helvetica", "bold");
      pdf.setFontSize(14);
      pdf.setTextColor(66, 33, 29);
      pdf.text("KWATERA", logoEl ? 28 : 15, 21);
      
      pdf.setFont("Helvetica", "bold");
      pdf.setFontSize(9);
      pdf.setTextColor(122, 122, 122);
      pdf.text(t('report.reservationsRegister'), 195, 21, { align: "right" });
      
      pdf.setDrawColor(218, 205, 202);
      pdf.setLineWidth(0.4);
      pdf.line(15, 28, 195, 28);

      // Add Register Summary Cards
      pdf.setFillColor(253, 253, 253);
      pdf.setDrawColor(218, 205, 202);
      pdf.setLineWidth(0.3);
      pdf.roundedRect(15, 34, 180, 18, 2, 2, "FD");

      // Draw vertical separators in the summary card
      pdf.line(75, 34, 75, 52);
      pdf.line(135, 34, 135, 52);

      // Card 1: Total Stays
      pdf.setFont("Helvetica", "bold");
      pdf.setFontSize(8);
      pdf.setTextColor(66, 33, 29);
      pdf.text(t('report.totalRegisteredStays'), 20, 40);
      pdf.setFont("Helvetica", "bold");
      pdf.setFontSize(10);
      pdf.setTextColor(26, 26, 26);
      pdf.text(t('report.reservationsCount', { count: activeReservations.length }), 20, 47);

      // Card 2: Total Bookings Value
      pdf.setFont("Helvetica", "bold");
      pdf.setFontSize(8);
      pdf.setTextColor(66, 33, 29);
      pdf.text(t('report.totalRegisterValue'), 80, 40);
      pdf.setFont("Helvetica", "bold");
      pdf.setFontSize(10);
      pdf.setTextColor(26, 26, 26);
      const totalRegisterValue = activeReservations.reduce((sum, r) => sum + (r.totalPrice ?? 0), 0);
      pdf.text(formatCurrency(totalRegisterValue), 80, 47);

      // Card 3: Avg Booking Value
      pdf.setFont("Helvetica", "bold");
      pdf.setFontSize(8);
      pdf.setTextColor(66, 33, 29);
      pdf.text(t('report.averageStayValue'), 140, 40);
      pdf.setFont("Helvetica", "bold");
      pdf.setFontSize(10);
      pdf.setTextColor(26, 26, 26);
      const avgVal = activeReservations.length > 0 ? totalRegisterValue / activeReservations.length : 0;
      pdf.text(formatCurrency(avgVal), 140, 47);

      pdf.setFont("Helvetica", "bold");
      pdf.setFontSize(10);
      pdf.setTextColor(66, 33, 29);
      pdf.text(t('report.reservationsLog'), 15, 60);

      pdf.setDrawColor(218, 205, 202);
      pdf.setLineWidth(0.4);
      pdf.line(15, 63, 195, 63);

      pdf.setFillColor(66, 33, 29);
      pdf.rect(15, 66, 180, 8, "F");

      pdf.setFont("Helvetica", "bold");
      pdf.setFontSize(8);
      pdf.setTextColor(255, 255, 255);
      pdf.text(t('report.numberShort'), 18, 71.5, { align: "center" });
      pdf.text(t('report.guestName'), 23, 71.5);
      pdf.text(t('report.unitName'), 53, 71.5);
      pdf.text(t('manualReservation.checkIn'), 83, 71.5);
      pdf.text(t('manualReservation.checkOut'), 102, 71.5);
      pdf.text(t('report.nights'), 123, 71.5, { align: "center" });
      pdf.text(t('report.pricePerNight'), 146, 71.5, { align: "right" });
      pdf.text(t('common.status'), 150, 71.5);
      pdf.text(t('checkout.totalPrice'), 193, 71.5, { align: "right" });

      let y = 74;

      activeReservations.forEach((res, index) => {
        if (y > 265) {
          pdf.addPage();
          y = 32;

          if (logoEl) {
            pdf.addImage(logoEl, "PNG", 15, 14, 10, 10);
          }
          pdf.setFont("Helvetica", "bold");
          pdf.setFontSize(14);
          pdf.setTextColor(66, 33, 29);
          pdf.text("KWATERA", logoEl ? 28 : 15, 21);
          pdf.setFont("Helvetica", "bold");
          pdf.setFontSize(9);
          pdf.setTextColor(122, 122, 122);
          pdf.text(t('report.reservationsRegisterContinued'), 195, 21, { align: "right" });
          pdf.setDrawColor(218, 205, 202);
          pdf.setLineWidth(0.4);
          pdf.line(15, 28, 195, 28);

          pdf.setFillColor(66, 33, 29);
          pdf.rect(15, 32, 180, 8, "F");
          pdf.setFont("Helvetica", "bold");
          pdf.setFontSize(8);
          pdf.setTextColor(255, 255, 255);
          pdf.text(t('report.numberShort'), 18, 37.5, { align: "center" });
          pdf.text(t('report.guestName'), 23, 37.5);
          pdf.text(t('report.unitName'), 53, 37.5);
          pdf.text(t('manualReservation.checkIn'), 83, 37.5);
          pdf.text(t('manualReservation.checkOut'), 102, 37.5);
          pdf.text(t('report.nights'), 123, 37.5, { align: "center" });
          pdf.text(t('report.pricePerNight'), 146, 37.5, { align: "right" });
          pdf.text(t('common.status'), 150, 37.5);
          pdf.text(t('checkout.totalPrice'), 193, 37.5, { align: "right" });

          y = 40;
        }

        pdf.setDrawColor(218, 205, 202);
        pdf.setLineWidth(0.2);
        pdf.line(15, y, 195, y);
        pdf.line(15, y + 8, 195, y + 8);

        pdf.line(15, y, 15, y + 8);
        pdf.line(21, y, 21, y + 8);
        pdf.line(51, y, 51, y + 8);
        pdf.line(81, y, 81, y + 8);
        pdf.line(100, y, 100, y + 8);
        pdf.line(119, y, 119, y + 8);
        pdf.line(127, y, 127, y + 8);
        pdf.line(148, y, 148, y + 8);
        pdf.line(169, y, 169, y + 8);
        pdf.line(195, y, 195, y + 8);

        const start = new Date(res.startDate);
        const end = new Date(res.endDate);
        const nights = Math.round((end.getTime() - start.getTime()) / (1000 * 3600 * 24));
        const pricePerNight = res.pricePerNightSnapshot || (res.totalPrice && nights > 0 ? res.totalPrice / nights : 0);

        pdf.setFont("Helvetica", "normal");
        pdf.setFontSize(8);
        pdf.setTextColor(26, 26, 26);
        pdf.text(String(index + 1), 18, y + 5, { align: "center" });
        pdf.text(res.guestName || "—", 23, y + 5);
        pdf.text(res.unitName || "—", 53, y + 5);
        pdf.text(res.startDate || "—", 83, y + 5);
        pdf.text(res.endDate || "—", 102, y + 5);
        pdf.text(String(nights), 123, y + 5, { align: "center" });
        pdf.text(pricePerNight ? formatCurrency(pricePerNight) : "—", 146, y + 5, { align: "right" });
        pdf.text(res.status ? t(`statuses.${res.status}`, { defaultValue: res.status }) : "—", 150, y + 5);
        pdf.setFont("Helvetica", "bold");
        pdf.text(res.totalPrice ? formatCurrency(res.totalPrice) : "—", 193, y + 5, { align: "right" });

        y += 8;
      });

      const totalPages = pdf.getNumberOfPages();
      for (let i = 1; i <= totalPages; i++) {
        pdf.setPage(i);
        
        pdf.setDrawColor(218, 205, 202);
        pdf.setLineWidth(0.3);
        pdf.line(15, 280, 195, 280);

        pdf.setFont("Helvetica", "normal");
        pdf.setFontSize(8);
        pdf.setTextColor(122, 122, 122);
        pdf.text(t('report.footer', { year: new Date().getFullYear() }), 15, 286);
        pdf.text(t('report.generatedOnValue', { value: new Date().toLocaleDateString(localeCode) }), 105, 286, { align: "center" });
        
        pdf.setFont("Helvetica", "bold");
        pdf.setTextColor(26, 26, 26);
        pdf.text(t('report.pageValue', { page: i, total: totalPages }), 195, 286, { align: "right" });
      }

      const startStr = formatDate(startDate);
      const endStr = formatDate(endDate);
      pdf.save(
        `kwatera-${userRole.toLowerCase()}-report-${startStr}-to-${endStr}.pdf`
      );
    } catch (error) {
      console.error("Failed to generate PDF report:", error);
        alert(t('report.error'));
    } finally {
      setIsGenerating(false);
      setStatusMessage("");
    }
  };

  return (
    <>
      <button
        type="button"
        onClick={handleDownloadCSV}
        className="px-4 py-2 text-sm font-bold text-[#42211D] bg-[#F7F7F7] border border-[#DACDCA] hover:bg-gray-100 rounded-lg transition-all shadow-sm cursor-pointer flex items-center gap-2 active:scale-[0.98]"
      >
        <FileText className="w-4 h-4" />
          {t('report.downloadCsv')}
      </button>

      <button
        type="button"
        disabled={isGenerating}
        onClick={handleDownloadPDF}
        className="px-5 py-2 text-sm font-bold text-white bg-[#42211D] hover:bg-[#341a17] rounded-lg transition-all shadow-md cursor-pointer flex items-center gap-2 active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {isGenerating ? (
          <Loader2 className="w-4 h-4 animate-spin" />
        ) : (
          <Download className="w-4 h-4" />
        )}
          {t('report.downloadPdf')}
      </button>

      {isGenerating && (
        <div className="fixed inset-0 bg-black/45 backdrop-blur-sm z-[9999] flex flex-col items-center justify-center text-white">
          <div className="bg-[#ffffff] text-[#1A1A1A] px-8 py-6 rounded-2xl shadow-2xl flex flex-col items-center gap-4 border border-[#DACDCA] max-w-sm text-center">
            <Loader2 className="w-8 h-8 text-[#42211D] animate-spin" />
            <h3 className="text-lg font-extrabold text-[#1A1A1A]">{t('report.generating')}</h3>
            <p className="text-sm text-[#7A7A7A] font-medium">{statusMessage}</p>
          </div>
        </div>
      )}
    </>
  );
}
