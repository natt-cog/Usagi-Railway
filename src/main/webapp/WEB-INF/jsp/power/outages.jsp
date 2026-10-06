<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="停電作業" />
<%@ include file="../common/header.jspf" %>
<h1>停電作業 (き電停止) 申請一覧</h1>
<div class="panel">
  <sec:authorize access="hasAnyRole('MAINTAINER','ADMIN')"><p><a class="button" href="${ctx}/power/outages/new">新規申請</a></p></sec:authorize>
  <table class="grid sortable" id="outages">
    <thead><tr><th>申請番号</th><th>作業日</th><th>時間帯</th><th>変電所</th><th>遮断器</th><th>作業内容</th><th>状態</th><th>申請者</th><th>承認者</th></tr></thead>
    <tbody>
    <c:forEach var="o" items="${outages}">
      <tr>
        <td><a href="${ctx}/power/outages/${o.requestNo}">${o.requestNo}</a></td>
        <td><fmt:formatDate value="${o.workDate}" pattern="yyyy/MM/dd" /></td>
        <td>${o.startTime}-${o.endTime}</td>
        <td><c:out value="${o.breaker.substation.name}" /></td>
        <td>${o.breaker.breakerCode}</td>
        <td><c:out value="${o.description}" /></td>
        <td class="st-${o.status}">${o.status.label}</td>
        <td><c:out value="${o.requestedBy}" /></td>
        <td><c:out value="${o.approvedBy}" /></td>
      </tr>
    </c:forEach>
    </tbody>
  </table>
</div>
<%@ include file="../common/footer.jspf" %>
