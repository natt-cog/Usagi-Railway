<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="停電作業 申請" />
<%@ include file="../common/header.jspf" %>
<h1>停電作業 (き電停止) 申請</h1>
<div class="panel">
<form action="${ctx}/power/outages" method="post" class="outage">
  <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
  <table class="kv">
    <tr><th>遮断器<span class="req">必須</span></th><td>
      <select name="breakerId" required>
        <c:forEach var="b" items="${breakers}">
          <option value="${b.id}">${b.substation.code} <c:out value="${b.substation.name}" /> ${b.breakerCode} <c:out value="${b.name}" /> (${b.state.label})</option>
        </c:forEach>
      </select></td></tr>
    <tr><th>作業日<span class="req">必須</span></th><td><input type="text" name="workDate" size="10" placeholder="yyyy/MM/dd" required></td></tr>
    <tr><th>作業時間<span class="req">必須</span></th><td><input type="text" name="startTime" size="5" placeholder="01:00" required> 〜 <input type="text" name="endTime" size="5" placeholder="04:00" required>
      <span class="note">終電後〜初電前 (同日内) で入力</span></td></tr>
    <tr><th>作業内容<span class="req">必須</span></th><td><input type="text" name="description" size="60" maxlength="200" required></td></tr>
  </table>
  <p><button type="submit" class="primary">申請</button> <a href="${ctx}/power/outages">戻る</a></p>
</form>
</div>
<%@ include file="../common/footer.jspf" %>
