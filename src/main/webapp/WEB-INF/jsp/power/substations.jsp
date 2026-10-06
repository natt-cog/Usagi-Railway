<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="変電所一覧" />
<%@ include file="../common/header.jspf" %>
<h1>変電所・き電区分所 一覧</h1>
<div class="panel">
<table class="grid sortable" id="substations">
  <thead><tr><th>コード</th><th>名称</th><th>種別</th><th>線区</th><th>キロ程</th><th>遮断器</th><th>最新 電圧 [V]</th><th>最新 電流 [A]</th></tr></thead>
  <tbody>
  <c:forEach var="s" items="${substations}">
    <c:set var="m" value="${latest[s.code]}" />
    <tr>
      <td><a href="${ctx}/power/substations/${s.code}">${s.code}</a></td>
      <td><c:out value="${s.name}" /></td>
      <td>${s.kind.label}</td>
      <td><c:out value="${s.lineName}" /></td>
      <td class="num"><fmt:formatNumber value="${s.kmPost}" minFractionDigits="3" /> km</td>
      <td><c:forEach var="b" items="${s.breakers}"><span class="bk-${b.state}">${b.breakerCode}:${b.state.label}</span> </c:forEach></td>
      <c:choose>
        <c:when test="${s.telemetryEnabled}">
          <td class="num"><fmt:formatNumber value="${m.voltageV}" /></td>
          <td class="num"><fmt:formatNumber value="${m.currentA}" /></td>
        </c:when>
        <c:otherwise><td colspan="2" class="empty">計測対象外</td></c:otherwise>
      </c:choose>
    </tr>
  </c:forEach>
  </tbody>
</table>
</div>
<%@ include file="../common/footer.jspf" %>
