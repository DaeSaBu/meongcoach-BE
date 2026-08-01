/**
 * 미디어 업로드 모듈. 클라이언트가 스토리지에 직접 업로드할 수 있는 URL 발급을 담당한다.
 * 이미지는 Cloudflare R2, 영상은 AWS S3의 presigned URL을 쓴다. 스토리지가 다르므로 어댑터와 설정을 분리한다.
 */
@ApplicationModule
package com.daesabu.meongcoach.media;

import org.springframework.modulith.ApplicationModule;
