package com.review.shop.service.admin;

import com.review.shop.dto.admin.MemberPointRow;
import com.review.shop.dto.user.UserSummaryDTO;
import com.review.shop.dto.userinfo.user_related.Point.PointHistoryDTO;
import com.review.shop.exception.ResourceNotFoundException;
import com.review.shop.exception.WrongRequestException;
import com.review.shop.repository.admin.AdminUserMapper;
import com.review.shop.repository.userinfo.user_related.PointMapper;
import jakarta.annotation.security.RolesAllowed;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
@RolesAllowed("ADMIN")
public class AdminUserService {

    private static final String ADMIN_BALANCE_DESCRIPTION = "관리자 잔액 수정";

    private final AdminUserMapper adminUserMapper;
    private final PointMapper pointMapper;

    // 포인트 조회
    public Integer getMemberPoints(int user_id) {
        Integer points = adminUserMapper.getMemberPoints(user_id);
        if (points == null) {
            throw new ResourceNotFoundException("포인트 정보를 찾을 수 없습니다.");
        }
        return points;
    }

    @Transactional
    public void updateMemberPoints(int user_id, Integer expectedPoints, Integer targetPoints) {
        if (targetPoints == null || targetPoints < 0) {
            throw new WrongRequestException("포인트는 0 이상이어야 합니다.");
        }
        if (expectedPoints == null || expectedPoints < 0) {
            throw new WrongRequestException("기준 잔액은 0 이상이어야 합니다.");
        }
        // selectForUpdate의 null은 행 없음과 POINT NULL을 구분하지 못한다.
        MemberPointRow locked = adminUserMapper.lockMemberPoint(user_id);
        if (locked == null) {
            throw new ResourceNotFoundException("포인트를 수정할 회원을 찾을 수 없습니다.");
        }
        int current = locked.getPoint() == null ? 0 : locked.getPoint();
        if (current != expectedPoints) {
            throw new WrongRequestException("현재 잔액이 요청한 기준 잔액과 다릅니다.");
        }
        if (current == targetPoints) {
            return;
        }
        int affected = adminUserMapper.updateMemberPoints(user_id, targetPoints);
        if (affected == 0) {
            throw new ResourceNotFoundException("포인트를 수정할 회원을 찾을 수 없습니다.");
        }
        PointHistoryDTO history = new PointHistoryDTO();
        history.setUser_id(user_id);
        history.setAmount(Math.abs(targetPoints - current));
        history.setType(targetPoints > current ? "EARN" : "USE");
        history.setDescription(ADMIN_BALANCE_DESCRIPTION);
        pointMapper.aboutPoint(history, null);
    }



    public List<UserSummaryDTO> getAllusers() {
        if(adminUserMapper.getAllusers() == null){
            throw new ResourceNotFoundException( "사용자 정보를 찾을 수 없습니다.");
        }

        return adminUserMapper.getAllusers();
    }

    // 밴 기록 추가
    public void setBan(int user_id, String reason) {
        int result = adminUserMapper.setBlacklist(user_id, reason);

        if(result == 0){
            throw new ResourceNotFoundException("밴 기록에 실패했습니다. 해당 사용자를 찾을 수 없습니다.");
        }
    }

    // 유저 탈퇴처리
    public void deleteUser(int user_id) {
        int result = adminUserMapper.deleteUser(user_id);

        if(result == 0){
            throw new ResourceNotFoundException("사용자 삭제에 실패했습니다. 해당 사용자를 찾을 수 없습니다.");
        }
    }

    // 유저의 밴 처리, 밴 기록 처리 트랜잭션
    @Transactional
    public void blockAndExpelUser(int user_id, String reason) {
        setBan(user_id, reason);
        deleteUser(user_id);
    }
}
