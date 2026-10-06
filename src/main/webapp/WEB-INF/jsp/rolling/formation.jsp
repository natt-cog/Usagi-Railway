<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="${formation.formationNo} 編成" />
<%@ include file="../common/header.jspf" %>
<h1>${formation.formationNo} 編成 <small><c:out value="${formation.series}" /> ${formation.carCount} 両 / <c:out value="${formation.depot}" /> / <span class="fs-${formation.status}">${formation.status.label}</span></small></h1>
<div class="row">
<div class="panel half">
  <h2>検査期限</h2>
  <table class="kv" id="inspection">
    <tr><th>交番検査</th><td>前回 <fmt:formatDate value="${formation.lastKobanOn}" pattern="yyyy/MM/dd" /> → 期限 ${due.kobanDueText}</td></tr>
    <tr><th>重要部検査</th><td>前回 <fmt:formatDate value="${formation.lastJuyobuOn}" pattern="yyyy/MM/dd" /> → 期限 ${due.juyobuDueText}
      (走行 <fmt:formatNumber value="${formation.kmSinceJuyobu}" /> / 600,000 km)</td></tr>
    <tr><th>全般検査</th><td>前回 <fmt:formatDate value="${formation.lastZenpanOn}" pattern="yyyy/MM/dd" /> → 期限 ${due.zenpanDueText}</td></tr>
    <tr><th>次回</th><td>${due.nextKind.label} ${due.nextDueText} (残 ${due.daysRemaining} 日)
      <span class="judge-${due.judge}">${due.judge.label}</span><c:if test="${due.kmExceeded}"> 走行km超過</c:if></td></tr>
    <tr><th>総走行距離</th><td><fmt:formatNumber value="${formation.totalKm}" /> km (営業開始 <fmt:formatDate value="${formation.inServiceOn}" pattern="yyyy/MM/dd" />)</td></tr>
  </table>
</div>
<div class="panel half">
  <h2>車両・搭載機器</h2>
  <table class="grid" id="equipment">
    <thead><tr><th>号車</th><th>車番</th><th>種別</th><th>製造番号</th><th>型式</th></tr></thead>
    <tbody>
    <c:forEach var="car" items="${formation.cars}">
      <c:choose>
        <c:when test="${empty car.equipment}">
          <tr><td class="num">${car.position}</td><td>${car.carNo} (${car.carType})</td><td colspan="3" class="empty">-</td></tr>
        </c:when>
        <c:otherwise>
          <c:forEach var="e" items="${car.equipment}">
            <tr><td class="num">${car.position}</td><td>${car.carNo} (${car.carType})</td><td>${e.equipmentType.label}</td>
                <td><a href="${ctx}/rolling/equipment/${e.serialNo}">${e.serialNo}</a></td><td>${e.model}</td></tr>
          </c:forEach>
        </c:otherwise>
      </c:choose>
    </c:forEach>
    </tbody>
  </table>
</div>
</div>
<div class="panel">
  <h2>故障履歴</h2>
  <table class="grid">
    <thead><tr><th>故障番号</th><th>発生日時</th><th>製造番号</th><th>内容</th><th>重要度</th><th>処置状況</th></tr></thead>
    <tbody>
    <c:forEach var="f" items="${failures}">
      <tr><td><a href="${ctx}/rolling/failures/${f.failureNo}">${f.failureNo}</a></td><td><fmt:formatDate value="${f.occurredAt}" pattern="yyyy/MM/dd HH:mm" /></td>
          <td>${f.equipment.serialNo}</td><td><c:out value="${f.symptom}" /></td><td>${f.severity.label}</td><td class="st-${f.status}">${f.status.label}</td></tr>
    </c:forEach>
    <c:if test="${empty failures}"><tr><td colspan="6" class="empty">故障履歴なし</td></tr></c:if>
    </tbody>
  </table>
</div>
<%@ include file="../common/footer.jspf" %>
