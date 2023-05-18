# myCloudPan
简易网盘项目，可以上传下载文件修改删除文件,可以增删改查文件夹，也可以通过分享链接给别人下载文件或者文件夹。





![image-20230518194444931](https://service-edu-2000.oss-cn-hangzhou.aliyuncs.com/pic_go_areaimage-20230518194444931.png)



![image-20230518194457449](https://service-edu-2000.oss-cn-hangzhou.aliyuncs.com/pic_go_areaimage-20230518194457449.png)


部署：项目中使用了自己做的一个工具库starter，已经放在项目的mystarter文件夹下，项目涉及到的sql在sql文件夹下
如果你要启动项目的话需要使用

```
mvn install:install-file -Dfile=lms-utils-1.0-SNAPSHOT.jar -Dmaven.repo.local=D:\apache-maven-3.6.1\maven_repository -DgroupId=com.lms -DartifactId=lms-utils -Dversion=1.0-SNAPSHOT -Dpackaging=jar

```
在jar包所在路径下使用这条命令
其中-Dmaven.repo.local指定你的maven本地仓库路径，

另外前端修改需要在.env.development修改你的后端地址

需求是每个用户都有自己独立的存储空间可以存储文件下载文件，创建文件夹，修改文件或文件夹名字，删除文件或者文件夹


关于文件夹表和文件表：
文件夹是虚拟路径，没有实际创建
文件存储的实际路径是根据用户bucket桶跟日期存放路径。主要目的是解决上传重复文件的问题
每个用户都有一个默认的路径根目录(/root)


- 用户注册：前端传递账号密码给后端，后端首先判断用户名是否存在，然后加密密码，记录用户信息，并且创建用户的根目录(/root) 然后分配配额(都是10G)
- 用户登录: 前端传递账号密码给后端，后端校验成功后将用户信息记录到session中，这样响应体会携带sessionid给前端，前端自动将sessionid设置到cookie中，要访问其他资源的时候都需要携带sessionid(前端配置请求设置携带cookie)
- 上传文件：
  前端上传multifile文件和当前的路径发送给后端
  后端获取文件和路径，再根据session获取用户id，判断文件夹是否存在,然后上传文件插入File表，同时修改文件夹大小，修改用户配额。
- 下载文件：
  前端发送文件名称， 后端去查找文件File表，找到文件，根据file_path获取文件，然后将文件流转换为二进制数据传递前端
- 创建文件夹：
  前端发送要创建的文件夹名和当前的路径给后端，后端查看同一路径下有没有重名文件夹，根据当前路径和文件夹名作为文件夹名插入folder表
- 修改文件名：
  前端发送文件id和新文件名给后端，后端根据id和新文件名修改文件
- 修改文件夹名：
  跟修改文件类似。
- 删除文件：
  前端传递文件id给后端，后端删除文件和记录的同时，还要修改用户配额，修改所属文件夹的大小
- 删除文件夹：前端传递文件夹id,后端首先根据文件夹id查出文件夹树，然后递归删除每一级文件夹所包含的文件和文件夹记录，最后修改用户配额
- 下载文件夹： 
  前端传递文件夹名（路径） ，后端接收然后得到文件夹的得到文件夹树，然后创建zip临时压缩文件，递归文件夹将文件夹添加进去，同时添加所包含的文件


