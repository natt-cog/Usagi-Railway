<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="${substation.name}" />
<%@ include file="../common/header.jspf" %>
<h1><c:out value="${substation.code}" /> <c:out value="${substation.name}" /> <small>${substation.kind.label} / <c:out value="${substation.lineName}" /> <fmt:formatNumber value="${substation.kmPost}" minFractionDigits="3" /> km / 直流 ${substation.feedVoltageV}V</small></h1>
<div class="row">
<div class="panel half">
  <h2>遮断器</h2>
  <table class="grid">
    <thead><tr><th>番号</th><th>名称</th><th>定格 [A]</th><th>状態</th></tr></thead>
    <tbody>
    <c:forEach var="b" items="${substation.breakers}">
      <tr><td>${b.breakerCode}</td><td><c:out value="${b.name}" /></td><td class="num"><fmt:formatNumber value="${b.ratedCurrentA}" /></td><td><span class="bk-${b.state}">${b.state.label}</span></td></tr>
    </c:forEach>
    </tbody>
  </table>
  <h2 style="margin-top:12px">警報履歴</h2>
  <table class="grid">
    <thead><tr><th>発生</th><th>レベル</th><th>コード</th><th>内容</th><th>確認</th></tr></thead>
    <tbody>
    <c:forEach var="a" items="${alarms}">
      <tr><td><fmt:formatDate value="${a.occurredAt}" pattern="MM/dd HH:mm" /></td><td><span class="lv-${a.level}">${a.level.label}</span></td>
          <td>${a.alarmCode}</td><td><c:out value="${a.message}" /></td><td>${a.acknowledged ? a.ackBy : '未確認'}</td></tr>
    </c:forEach>
    <c:if test="${empty alarms}"><tr><td colspan="5" class="empty">警報なし</td></tr></c:if>
    </tbody>
  </table>
</div>
<div class="panel half">
  <h2>計測値 (1 時間値)</h2>
  <form method="get" class="inline-form">
    <label>計測日 <input type="text" name="date" value="${date}" size="10"></label>
    <button type="submit">表示</button>
    <span class="note">閾値: 電圧 ${uvThreshold}V 未満 / 電流 ${ocThreshold}A 超過</span>
  </form>
  <table class="grid" id="measurements">
    <thead><tr><th>時刻</th><th>電圧 [V]</th><th>電流 [A]</th><th>電力量 [kWh]</th></tr></thead>
    <tbody>
    <c:forEach var="m" items="${measurements}">
      <tr class="${m.missing ? 'missing' : ''}">
        <td><fmt:formatDate value="${m.measuredAt}" pattern="HH:mm" /></td>
        <c:choose>
          <c:when test="${m.missing}"><td colspan="3" class="empty">欠測</td></c:when>
          <c:otherwise>
            <td class="num ${m.voltageV lt uvThreshold ? 'over' : ''}"><fmt:formatNumber value="${m.voltageV}" /></td>
            <td class="num ${m.currentA gt ocThreshold ? 'over' : ''}"><fmt:formatNumber value="${m.currentA}" /></td>
            <td class="num"><fmt:formatNumber value="${m.energyKwh}" /></td>
          </c:otherwise>
        </c:choose>
      </tr>
    </c:forEach>
    <c:if test="${empty measurements}"><tr><td colspan="4" class="empty">計測値なし</td></tr></c:if>
    </tbody>
  </table>
</div>
</div>
<%@ include file="../common/footer.jspf" %>
