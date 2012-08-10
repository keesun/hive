package models;

import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.avaje.ebean.Ebean;

import play.db.ebean.Model;
import play.db.ebean.Model.Finder;

/**
 * @author "Hwi Ahn"
 *
 */
@Entity
public class Permission extends Model{
    private static final long serialVersionUID = 1L;
    private static Finder<Long, Permission> find = new Finder<Long, Permission>(
            Long.class, Permission.class);
    
    @Id
    public Long id;
    
    public String resource;
    public String operation;
    
    @ManyToMany
    public List<Role> roles;
    
    /**
     * 해당 유저가 해당 프로젝트에서 해당 리소스와 오퍼레이션을 위한 퍼미션을 가지고 있는지 확인합니다.
     * 
     * @param userId
     * @param projectId
     * @param resource
     * @param operation
     * @return
     */
    public static boolean permissionCheck(Long userId, Long projectId,
            String resource, String operation) {
        int findRowCount = find.where()
                .eq("roles.projectUsers.user.id", userId)
                .eq("roles.projectUsers.project.id", projectId)
                .eq("resource", resource)
                .eq("operation", operation)
                .findRowCount();
        return (findRowCount != 0) ? true : false;
    }
    
    /**
     * 해당 롤이 가지고 있는 퍼미션들의 리스트를 반환합니다.
     * 
     * @param roleId
     * @return
     */
    public static List<Permission> findPermissionsByRole(Long roleId) {
        return find.where()
                .eq("roles.id", roleId).findList();
    }
}
