import {
  setToken,
  setRefreshToken,
  removeToken,
  removeRefreshToken,
  getRefreshToken,
} from '@/utils/auth';
import { setStore, getStore } from '@/utils/store';
import { validatenull } from '@/utils/validate';
import { deepClone } from '@/utils/util';
import {
  loginByUsername,
  loginBySocial,
  loginBySso,
  loginByPhone,
  getUserInfo,
  logout,
  refreshToken,
  getButtons,
  registerUser,
} from '@/api/user';
import { getRoutes, getMainMenu, getTopMenu } from '@/api/system/menu';
import { formatPath } from '@/router/avue-router';
import { ElMessage } from 'element-plus';
import { encrypt } from '@/utils/sm2';
import website from '@/config/website';

const menuProps = website.menu;
const childrenKey = menuProps.children;
const fixedHomePath = website.fistPage.path;

const formatRouteMenu = data => {
  const routeMenu = deepClone(data || []);
  routeMenu.forEach(ele => formatPath(ele, true));
  return routeMenu;
};

const isFixedHomeMenu = menuItem => menuItem && menuItem[menuProps.path] === fixedHomePath;

const formatTopMenuList = data =>
  deepClone(data || [])
    .filter(item => item && !isFixedHomeMenu(item))
    .map(item => ({
      ...item,
      [menuProps.icon]: item[menuProps.icon] || menuProps.iconDefault,
      [childrenKey]: item[childrenKey] || [],
    }));

const user = {
  state: {
    tenantId: getStore({ name: 'tenantId' }) || '',
    userInfo: getStore({ name: 'userInfo' }) || [],
    permission: getStore({ name: 'permission' }) || {},
    roles: [],
    menuId: {},
    routesLoaded: false,
    topMenu: [],
    menu: getStore({ name: 'menu' }) || [],
    menuAll: getStore({ name: 'menuAll' }) || [],
    token: getStore({ name: 'token' }) || '',
    refreshToken: getStore({ name: 'refreshToken' }) || '',
  },
  actions: {
    //根据用户名登录
    LoginByUsername({ commit }, userInfo = {}) {
      return new Promise((resolve, reject) => {
        loginByUsername(
          userInfo.tenantId,
          userInfo.deptId,
          userInfo.roleId,
          userInfo.username,
          encrypt(userInfo.password),
          userInfo.type,
          userInfo.key,
          userInfo.code
        )
          .then(res => {
            const data = res.data;
            if (data.error_description) {
              ElMessage({
                message: data.error_description,
                type: 'error',
              });
            } else {
              commit('SET_TOKEN', data.access_token);
              commit('SET_REFRESH_TOKEN', data.refresh_token);
              commit('SET_TENANT_ID', data.tenant_id);
              commit('SET_USER_INFO', data);
              commit('DEL_ALL_TAG');
              commit('CLEAR_LOCK');
            }
            resolve();
          })
          .catch(err => {
            reject(err);
          });
      });
    },
    //根据第三方信息登录
    LoginBySocial({ commit }, userInfo) {
      return new Promise((resolve, reject) => {
        loginBySocial(userInfo.tenantId, userInfo.source, userInfo.code, userInfo.state)
          .then(res => {
            const data = res.data;
            if (data.error_description) {
              ElMessage({
                message: data.error_description,
                type: 'error',
              });
            } else {
              commit('SET_TOKEN', data.access_token);
              commit('SET_REFRESH_TOKEN', data.refresh_token);
              commit('SET_USER_INFO', data);
              commit('SET_TENANT_ID', data.tenant_id);
              commit('DEL_ALL_TAG');
              commit('CLEAR_LOCK');
            }
            resolve();
          })
          .catch(err => {
            reject(err);
          });
      });
    },
    //根据单点信息登录
    LoginBySso({ commit }, userInfo) {
      return new Promise((resolve, reject) => {
        loginBySso(userInfo.state, userInfo.code)
          .then(res => {
            const data = res.data;
            if (data.error_description) {
              ElMessage({
                message: data.error_description,
                type: 'error',
              });
            } else {
              commit('SET_TOKEN', data.access_token);
              commit('SET_REFRESH_TOKEN', data.refresh_token);
              commit('SET_USER_INFO', data);
              commit('SET_TENANT_ID', data.tenant_id);
              commit('DEL_ALL_TAG');
              commit('CLEAR_LOCK');
            }
            resolve();
          })
          .catch(err => {
            reject(err);
          });
      });
    },
    //根据手机信息登录
    LoginByPhone({ commit }, userInfo) {
      return new Promise((resolve, reject) => {
        loginByPhone(
          userInfo.tenantId,
          encrypt(userInfo.phone),
          userInfo.codeId,
          userInfo.codeValue
        )
          .then(res => {
            const data = res.data;
            if (data.error_description) {
              ElMessage({
                message: data.error_description,
                type: 'error',
              });
            } else {
              commit('SET_TOKEN', data.access_token);
              commit('SET_REFRESH_TOKEN', data.refresh_token);
              commit('SET_USER_INFO', data);
              commit('SET_TENANT_ID', data.tenant_id);
              commit('DEL_ALL_TAG');
              commit('CLEAR_LOCK');
            }
            resolve();
          })
          .catch(err => {
            reject(err);
          });
      });
    },
    //用户注册
    RegisterUser({ commit }, userInfo = {}) {
      return new Promise((resolve, reject) => {
        registerUser(
          userInfo.tenantId,
          userInfo.name,
          userInfo.account,
          encrypt(userInfo.password),
          userInfo.phone,
          userInfo.email
        ).then(res => {
          const data = res.data;
          if (data.error_description) {
            ElMessage({
              message: data.error_description,
              type: 'error',
            });
            reject(data.error_description);
          } else {
            commit('SET_TOKEN', data.access_token);
            commit('SET_REFRESH_TOKEN', data.refresh_token);
            commit('SET_USER_INFO', data);
            commit('SET_TENANT_ID', data.tenant_id);
            commit('DEL_ALL_TAG');
            commit('CLEAR_LOCK');
          }
          resolve();
        });
      });
    },
    GetUserInfo({ commit }) {
      return new Promise((resolve, reject) => {
        getUserInfo()
          .then(res => {
            const data = res.data.data;
            commit('SET_ROLES', data.roles);
            resolve(data);
          })
          .catch(err => {
            reject(err);
          });
      });
    },
    //刷新token
    RefreshToken({ state, commit }, userInfo) {
      return new Promise((resolve, reject) => {
        refreshToken(
          getRefreshToken(),
          state.tenantId,
          !validatenull(userInfo) ? userInfo.deptId : state.userInfo.dept_id,
          !validatenull(userInfo) ? userInfo.roleId : state.userInfo.role_id
        )
          .then(res => {
            const data = res.data;
            commit('SET_TOKEN', data.access_token);
            commit('SET_REFRESH_TOKEN', data.refresh_token);
            commit('SET_USER_INFO', data);
            resolve();
          })
          .catch(error => {
            reject(error);
          });
      });
    },
    // 登出
    LogOut({ commit }) {
      return new Promise((resolve, reject) => {
        logout()
          .then(() => {
            commit('SET_TOKEN', '');
            commit('SET_MENU_ALL_NULL', []);
            commit('SET_TOP_MENU', []);
            commit('SET_MENU', []);
            commit('SET_ROLES', []);
            commit('DEL_ALL_TAG', []);
            commit('CLEAR_LOCK');
            removeToken();
            removeRefreshToken();
            removeToken();
            resolve();
          })
          .catch(error => {
            reject(error);
          });
      });
    },
    //注销session
    FedLogOut({ commit }) {
      return new Promise(resolve => {
        commit('SET_TOKEN', '');
        commit('SET_MENU_ALL_NULL', []);
        commit('SET_TOP_MENU', []);
        commit('SET_MENU', []);
        commit('SET_ROLES', []);
        commit('DEL_ALL_TAG', []);
        commit('CLEAR_LOCK');
        removeToken();
        removeRefreshToken();
        removeToken();
        resolve();
      });
    },
    GetTopMenu({ commit, state }) {
      return new Promise(resolve => {
        getTopMenu()
          .then(res => {
            const topMenu = formatTopMenuList(res.data.data);
            commit('SET_TOP_MENU', topMenu);
            resolve(topMenu);
          })
          .catch(() => {
            resolve(state.topMenu || []);
          });
      });
    },
    GetMainMenu() {
      return new Promise(resolve => {
        getMainMenu()
          .then(res => {
            resolve(res.data.data || {});
          })
          .catch(() => {
            resolve({});
          });
      });
    },
    GetMenu({ commit, dispatch, state }, topMenuId) {
      return new Promise(resolve => {
        const setSideMenu = menuList => {
          const sideMenu = menuList.filter(item => !isFixedHomeMenu(item));
          commit('SET_MENU', sideMenu);
        };
        const applyMenu = routeMenu => {
          commit('SET_MENU_ALL_NULL');
          commit('SET_MENU_ALL', routeMenu);
          dispatch('GetButtons');
          if (!topMenuId) {
            setSideMenu(routeMenu);
            resolve(routeMenu);
            return;
          }
          getRoutes(topMenuId)
            .then(res => {
              const topRouteMenu = formatRouteMenu(res.data.data);
              setSideMenu(topRouteMenu);
              resolve(topRouteMenu);
            })
            .catch(() => {
              setSideMenu([]);
              resolve([]);
            });
        };
        if (state.routesLoaded && (state.menuAll || []).length) {
          dispatch('GetButtons');
          if (!topMenuId) {
            setSideMenu(state.menuAll);
            resolve(state.menuAll);
            return;
          }
          getRoutes(topMenuId)
            .then(res => {
              const topRouteMenu = formatRouteMenu(res.data.data);
              setSideMenu(topRouteMenu);
              resolve(topRouteMenu);
            })
            .catch(() => {
              setSideMenu([]);
              resolve([]);
            });
          return;
        }
        getRoutes()
          .then(res => {
            applyMenu(formatRouteMenu(res.data.data));
          })
          .catch(() => {
            resolve([]);
          });
      });
    },
    GetButtons({ commit }) {
      return new Promise(resolve => {
        getButtons().then(res => {
          const data = res.data.data;
          commit('SET_PERMISSION', data);
          resolve();
        });
      });
    },
  },
  mutations: {
    SET_TOKEN: (state, token) => {
      setToken(token);
      state.token = token;
      setStore({ name: 'token', content: state.token });
    },
    SET_REFRESH_TOKEN: (state, refreshToken) => {
      setRefreshToken(refreshToken);
      state.refreshToken = refreshToken;
      setStore({ name: 'refreshToken', content: state.refreshToken });
    },
    SET_MENU_ID(state, menuId) {
      state.menuId = menuId;
    },
    SET_TOP_MENU: (state, topMenu) => {
      state.topMenu = topMenu;
      setStore({ name: 'topMenu', content: state.topMenu });
    },
    SET_TENANT_ID: (state, tenantId) => {
      state.tenantId = tenantId;
      setStore({ name: 'tenantId', content: state.tenantId });
    },
    SET_USER_INFO: (state, userInfo) => {
      if (validatenull(userInfo.user_id) && validatenull(userInfo.account)) {
        state.userInfo = { user_name: 'unauth', role_name: 'unauth', authority: 'unauth' };
      } else {
        if (validatenull(userInfo.avatar)) {
          userInfo.avatar = '/img/bg/img-logo.png';
        }
        if (!validatenull(userInfo.role_name)) {
          userInfo.roleName = userInfo.role_name;
          userInfo.authority = userInfo.role_name;
        }
        if (!validatenull(userInfo.user_id)) {
          userInfo.userId = userInfo.user_id;
        }
        if (!validatenull(userInfo.user_name)) {
          userInfo.userName = userInfo.user_name;
        }
        if (!validatenull(userInfo.tenant_id)) {
          userInfo.tenantId = userInfo.tenant_id;
        }
        if (!validatenull(userInfo.dept_id)) {
          userInfo.deptId = userInfo.dept_id;
        }
        if (!validatenull(userInfo.role_id)) {
          userInfo.roleId = userInfo.role_id;
        }
        if (!validatenull(userInfo.oauth_id)) {
          userInfo.oauthId = userInfo.oauth_id;
        }
        state.userInfo = userInfo;
      }
      setStore({ name: 'userInfo', content: state.userInfo });
    },
    SET_MENU_ALL: (state, menuAll) => {
      let menu = state.menuAll;
      menuAll.forEach(ele => {
        let index = menu.findIndex(item => item.path === ele.path);
        if (index === -1) {
          menu.push(ele);
        } else {
          menu[index] = ele;
        }
      });
      state.menuAll = menu;
      state.routesLoaded = true;
      setStore({ name: 'menuAll', content: state.menuAll });
    },
    SET_MENU_ALL_NULL: state => {
      state.menuAll = [];
      state.routesLoaded = false;
      setStore({ name: 'menuAll', content: state.menuAll });
    },
    SET_MENU: (state, menu) => {
      state.menu = menu;
      setStore({ name: 'menu', content: state.menu });
    },
    SET_ROLES: (state, roles) => {
      state.roles = roles;
    },
    SET_PERMISSION: (state, permission) => {
      let result = [];

      function getCode(list) {
        list.forEach(ele => {
          if (typeof ele === 'object') {
            const children = ele.children;
            const code = ele.code;
            if (children && children.length > 0) {
              getCode(children);
            } else {
              result.push(code);
            }
          }
        });
      }

      getCode(permission);
      state.permission = {};
      result.forEach(ele => {
        state.permission[ele] = true;
      });
      setStore({ name: 'permission', content: state.permission, type: 'session' });
    },
  },
};
export default user;
