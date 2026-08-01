/**
 * 미디어 업로드 모듈. 클라이언트가 스토리지에 직접 업로드할 수 있는 URL 발급을 담당한다.
 * 이미지는 Cloudflare R2 presigned URL을 쓰며, 추후 영상(Vimeo) 업로드도 이 모듈이 맡는다.
 */
@ApplicationModule
package com.daesabu.meongcoach.media;

import org.springframework.modulith.ApplicationModule;
