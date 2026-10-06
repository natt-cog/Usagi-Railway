<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="${equipment.serialNo}" />
<%@ include file="../common/header.jspf" %>
<h1>機器 ${equipment.serialNo} <small>${equipment.equipmentType.label} ${equipment.model}</small></h1>
<div class="panel">
  <table class="kv" id="equipment">
    <tr><th>製造番号</th><td>${equipment.serialNo}</td></tr>
    <tr><th>種別</th><td>${equipment.equipmentType.label}</td></tr>
    <tr><th>型式</th><td>${equipment.model}</td></tr>
    <tr><th>メーカー</th><td><c:out value="${equipment.maker}" /></td></tr>
    <tr><th>製造日</th><td><fmt:formatDate value="${equipment.manufacturedOn}" pattern="yyyy/MM/dd" /></td></tr>
    <tr><th>搭載位置</th><td>
      <c:choose>
        <c:when test="${empty equipment.car}">予備品 (未搭載)</c:when>
        <c:otherwise><a href="${ctx}/rolling/formations/${equipment.car.formation.formationNo}">${equipment.car.formation.formationNo}</a> 編成
          ${equipment.car.position} 号車 (${equipment.car.carNo}) / 搭載 <fmt:formatDate value="${equipment.installedOn}" pattern="yyyy/MM/dd" /></c:otherwise>
      </c:choose></td></tr>
  </table>
  <sec:authorize access="hasAnyRole('MAINTAINER','ADMIN')">
    <c:if test="${not empty equipment.car}"><p><a class="button" href="${ctx}/rolling/failures/new?serialNo=${equipment.serialNo}">この機器の故障を登録</a></p></c:if>
  </sec:authorize>
</div>
<div class="panel">
  <h2>故障・修理履歴</h2>
  <table class="grid">
    <thead><tr><th>故障番号</th><th>発生日時</th><th>編成</th><th>内容</th><th>重要度</th><th>処置状況</th></tr></thead>
    <tbody>
    <c:forEach var="f" items="${failures}">
      <tr><td><a href="${ctx}/rolling/failures/${f.failureNo}">${f.failureNo}</a></td><td><fmt:formatDate value="${f.occurredAt}" pattern="yyyy/MM/dd HH:mm" /></td>
          <td>${f.formationNo}</td><td><c:out value="${f.symptom}" /></td><td>${f.severity.label}</td><td class="st-${f.status}">${f.status.label}</td></tr>
    </c:forEach>
    <c:if test="${empty failures}"><tr><td colspan="6" class="empty">履歴なし</td></tr></c:if>
    </tbody>
  </table>
</div>
<%@ include file="../common/footer.jspf" %>
