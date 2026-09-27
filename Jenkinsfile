// 后端流水线（独立仓库可用，只负责 Java API 镜像）
// 与前端仓库的约定：容器网络 demo-net，本服务以别名 backend 提供，端口 8080
pipeline {
    agent any

    environment {
        IMAGE_NAME     = 'demo-java-api'
        IMAGE_TAG      = "${BUILD_NUMBER}-${env.GIT_COMMIT?.take(7) ?: 'local'}"
        // 本地私有仓库，存放构建好的镜像（供 docker compose / 回滚使用）
        LOCAL_REGISTRY = 'localhost:5000'
        // 基础镜像仓库：已预推入 eclipse-temurin:11-jdk / 11-jre
        BASE_REGISTRY  = 'localhost:5000'
        NET_NAME       = 'demo-net'
        CONTAINER      = 'demo-backend'
    }

    stages {
        stage('检出代码') {
            steps {
                checkout scm
            }
        }

        stage('构建镜像') {
            steps {
                script {
                    docker.build("${IMAGE_NAME}:${IMAGE_TAG}",
                                 "-f Dockerfile --build-arg BASE_REGISTRY=${BASE_REGISTRY} .")
                }
            }
        }

        stage('推送本地仓库') {
            steps {
                sh '''
                    docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${LOCAL_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}
                    docker push ${LOCAL_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}
                '''
            }
        }

        stage('部署运行') {
            steps {
                script {
                    // 1. 容器网络（前端 nginx 按别名 backend 访问本服务）
                    sh "docker network create ${NET_NAME} 2>/dev/null || true"

                    // 2. 替换容器
                    sh "docker rm -f ${CONTAINER} || true"
                    sh "docker run -d --name ${CONTAINER} --network ${NET_NAME} --network-alias backend --restart unless-stopped ${IMAGE_NAME}:${IMAGE_TAG}"

                    // 3. 清理本仓库的旧镜像（保留本次新镜像）
                    sh '''
                        docker images --format "{{.Repository}}:{{.Tag}}" ${IMAGE_NAME} \
                          | grep -v "${IMAGE_TAG}" \
                          | xargs -r docker rmi -f || true
                    '''

                    // 4. 健康检查（后端不暴露宿主机端口，看运行日志确认启动）
                    sh 'sleep 2'
                    sh 'docker logs ${CONTAINER}'
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            echo "后端部署成功：${IMAGE_NAME}:${IMAGE_TAG}"
        }
        failure {
            echo '后端构建失败，请查看日志'
        }
    }
}
