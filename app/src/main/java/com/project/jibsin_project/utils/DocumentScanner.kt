package com.project.jibsin_project.utils

import android.graphics.Bitmap
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

class DocumentScanner {
    init {
        if (!OpenCVLoader.initDebug()) {
            throw IllegalStateException("OpenCV initialization failed")
        }
    }

    suspend fun scanDocument(bitmap: Bitmap): Bitmap = withContext(Dispatchers.IO) {
        try {
            // 원본 크기 저장
            val originalWidth = bitmap.width
            val originalHeight = bitmap.height

            // Bitmap을 Mat으로 변환
            val originalMat = Mat()
            Utils.bitmapToMat(bitmap, originalMat)

            // 작업용 크기 조정
            val workingMat = Mat()
            val maxDim = 2000.0
            val scale = if (max(originalWidth, originalHeight) > maxDim) {
                maxDim / max(originalWidth, originalHeight)
            } else {
                1.0
            }

            if (scale != 1.0) {
                Imgproc.resize(originalMat, workingMat, Size(originalWidth * scale, originalHeight * scale))
            } else {
                originalMat.copyTo(workingMat)
            }

            // 이미지 전처리
            val preprocessedMat = preprocessImage(workingMat)

            // 문서 윤곽선 찾기
            val documentContour = findDocumentContours(preprocessedMat, workingMat.size())

            if (documentContour != null) {
                // 원근 변환 및 이미지 보정
                val correctedMat = perspectiveTransform(originalMat, documentContour, scale)

                // 이미지 품질 개선
                val enhancedMat = enhanceImage(correctedMat)

                // Mat을 Bitmap으로 변환
                val resultBitmap = Bitmap.createBitmap(
                    originalWidth,
                    originalHeight,
                    Bitmap.Config.ARGB_8888
                )
                Utils.matToBitmap(enhancedMat, resultBitmap)
                return@withContext resultBitmap
            }

            bitmap
        } catch (e: Exception) {
            throw e
        }
    }

    private fun preprocessImage(input: Mat): Mat {
        val result = Mat()

        // 그레이스케일 변환
        Imgproc.cvtColor(input, result, Imgproc.COLOR_BGR2GRAY)

        // 노이즈 제거
        Imgproc.GaussianBlur(result, result, Size(5.0, 5.0), 0.0)

        // 적응형 임계값 적용
        Imgproc.adaptiveThreshold(
            result,
            result,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY,
            11,
            2.0
        )

        // Canny 엣지 검출
        Imgproc.Canny(result, result, 50.0, 200.0)

        // 엣지 강화
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
        Imgproc.dilate(result, result, kernel)

        return result
    }

    private fun findDocumentContours(edges: Mat, size: Size): MatOfPoint? {
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()

        Imgproc.findContours(
            edges,
            contours,
            hierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        // 면적 기준으로 정렬
        contours.sortByDescending { Imgproc.contourArea(it) }

        val minArea = size.area() * 0.2

        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area < minArea) continue

            val perimeter = Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(
                MatOfPoint2f(*contour.toArray()),
                approx,
                0.02 * perimeter,
                true
            )

            if (approx.total() == 4L) {
                return MatOfPoint(*approx.toArray())
            }
        }

        return null
    }

    private fun perspectiveTransform(input: Mat, contour: MatOfPoint, scale: Double): Mat {
        val points = contour.toArray()
        val sortedPoints = sortPoints(points)

        // A4 비율 계산 (1:1.414)
        val width = input.width()
        val height = (width * 1.414).toInt()

        val src = MatOfPoint2f(*sortedPoints.map {
            Point(it.x / scale, it.y / scale)
        }.toTypedArray())

        val dst = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(width.toDouble(), 0.0),
            Point(width.toDouble(), height.toDouble()),
            Point(0.0, height.toDouble())
        )

        val transform = Imgproc.getPerspectiveTransform(src, dst)
        val result = Mat()
        Imgproc.warpPerspective(
            input,
            result,
            transform,
            Size(width.toDouble(), height.toDouble())
        )

        return result
    }

    private fun enhanceImage(input: Mat): Mat {
        val result = Mat()

        // 샤프닝
        val kernel = Mat(3, 3, CvType.CV_32F).apply {
            put(0, 0,
                -1.0, -1.0, -1.0,
                -1.0,  9.0, -1.0,
                -1.0, -1.0, -1.0
            )
        }
        Imgproc.filter2D(input, result, -1, kernel)

        // 대비 향상
        val lab = Mat()
        Imgproc.cvtColor(result, lab, Imgproc.COLOR_BGR2Lab)
        val channels = ArrayList<Mat>()
        Core.split(lab, channels)
        Core.normalize(channels[0], channels[0], 0.0, 255.0, Core.NORM_MINMAX)
        Core.merge(channels, lab)
        Imgproc.cvtColor(lab, result, Imgproc.COLOR_Lab2BGR)

        return result
    }

    private fun sortPoints(points: Array<Point>): Array<Point> {
        val center = Point(points.map { it.x }.average(), points.map { it.y }.average())
        return points.sortedWith(compareBy<Point> {
            val angle = Math.atan2(it.y - center.y, it.x - center.x)
            (angle + 2 * Math.PI) % (2 * Math.PI)
        }).toTypedArray()
    }
}