package com.project.jibsin_project.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.sqrt

class DocumentScanner {
    init {
        if (!OpenCVLoader.initDebug()) {
            throw IllegalStateException("OpenCV initialization failed")
        }
    }

    suspend fun scanDocument(bitmap: Bitmap): Bitmap = withContext(Dispatchers.IO) {
        try {
            // EXIF 정보를 유지하면서 이미지 방향 보정
            val rotatedBitmap = correctImageOrientation(bitmap)

            // Bitmap을 Mat으로 변환
            val originalMat = Mat()
            Utils.bitmapToMat(rotatedBitmap, originalMat)

            // 이미지 전처리
            val preprocessedMat = preprocessImage(originalMat)

            // 문서 윤곽선 찾기
            val documentContour = findDocumentContours(preprocessedMat, originalMat.size())

            if (documentContour != null) {
                // 원근 변환 및 크롭
                val transformedMat = perspectiveTransform(originalMat, documentContour)

                // 이미지 향상
                val enhancedMat = enhanceImage(transformedMat)

                // 결과 이미지 생성
                val resultBitmap = Bitmap.createBitmap(
                    enhancedMat.width(),
                    enhancedMat.height(),
                    Bitmap.Config.ARGB_8888
                )
                Utils.matToBitmap(enhancedMat, resultBitmap)

                // 최종 이미지 방향 확인 및 수정
                return@withContext ensurePortraitOrientation(resultBitmap)
            }
            rotatedBitmap
        } catch (e: Exception) {
            throw e
        }
    }

    private fun ensurePortraitOrientation(bitmap: Bitmap): Bitmap {
        return if (bitmap.width > bitmap.height) {
            val matrix = Matrix()
            matrix.postRotate(90f)
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    private fun correctImageOrientation(bitmap: Bitmap): Bitmap {
        // 이미지 방향 강제로 세로로 설정
        return if (bitmap.width > bitmap.height) {
            val matrix = Matrix()
            matrix.postRotate(90f)
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    private fun preprocessImage(input: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(input, gray, Imgproc.COLOR_BGR2GRAY)

        // 노이즈 제거
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

        // 이진화
        val binary = Mat()
        Imgproc.adaptiveThreshold(
            gray,
            binary,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY,
            11,
            2.0
        )

        // 엣지 검출
        val edges = Mat()
        Imgproc.Canny(binary, edges, 50.0, 200.0)

        // 윤곽선 강화
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        Imgproc.dilate(edges, edges, kernel)

        return edges
    }

    private fun findDocumentContours(edges: Mat, size: Size): MatOfPoint? {
        val contours = ArrayList<MatOfPoint>()
        Imgproc.findContours(
            edges,
            contours,
            Mat(),
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        // 면적 기준 더 엄격하게 조정
        val minArea = size.area() * 0.4  // 0.3에서 0.4로 증가
        val maxArea = size.area() * 0.98 // 0.95에서 0.98로 증가

        val validContours = contours
            .filter { contour ->
                val area = Imgproc.contourArea(contour)
                area in minArea..maxArea
            }
            .sortedByDescending { Imgproc.contourArea(it) }

        for (contour in validContours) {
            val peri = Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(
                MatOfPoint2f(*contour.toArray()),
                approx,
                0.02 * peri,
                true
            )

            if (approx.total() == 4L) {
                val points = approx.toArray()
                if (isRectangleValid(points)) {
                    // 경계 약간 안쪽으로 조정
                    val adjustedPoints = adjustPoints(points, size)
                    return MatOfPoint(*adjustedPoints)
                }
            }
        }
        return null
    }

    private fun adjustPoints(points: Array<Point>, size: Size): Array<Point> {
        // 경계를 안쪽으로 약간 조정하는 상수
        val padding = min(size.width, size.height) * 0.005

        return points.map { point ->
            val x = when {
                point.x < size.width / 2 -> point.x + padding  // 왼쪽 경계
                else -> point.x - padding  // 오른쪽 경계
            }
            val y = when {
                point.y < size.height / 2 -> point.y + padding  // 위쪽 경계
                else -> point.y - padding  // 아래쪽 경계
            }
            Point(x, y)
        }.toTypedArray()
    }

    private fun isRectangleValid(points: Array<Point>): Boolean {
        // 각도 허용 범위를 더 엄격하게 조정
        val angles = calculateAngles(points)
        if (!angles.all { it in 85.0..95.0 }) return false  // 90도에 더 가깝게

        // 변의 길이 비율 검사
        val edges = points.indices.map { i ->
            val next = (i + 1) % 4
            val dx = points[next].x - points[i].x
            val dy = points[next].y - points[i].y
            sqrt(dx * dx + dy * dy)
        }

        val minEdge = edges.minOrNull() ?: return false
        val maxEdge = edges.maxOrNull() ?: return false

        // 비율 허용 범위를 더 엄격하게 조정
        return maxEdge / minEdge < 1.2
    }

    private fun isRectangleReasonable(points: Array<Point>): Boolean {
        // 사각형의 각도가 적절한지 확인
        val angles = calculateAngles(points)
        return angles.all { angle ->
            angle in 70.0..110.0  // 90도에서 ±20도 허용
        }
    }

    private fun calculateAngles(points: Array<Point>): List<Double> {
        val angles = mutableListOf<Double>()
        for (i in points.indices) {
            val p1 = points[i]
            val p2 = points[(i + 1) % 4]
            val p3 = points[(i + 2) % 4]

            val angle = calculateAngle(p1, p2, p3)
            angles.add(angle)
        }
        return angles
    }

    private fun calculateAngle(p1: Point, p2: Point, p3: Point): Double {
        val v1 = Point(p1.x - p2.x, p1.y - p2.y)
        val v2 = Point(p3.x - p2.x, p3.y - p2.y)

        val dot = v1.x * v2.x + v1.y * v2.y
        val v1Mag = Math.sqrt(v1.x * v1.x + v1.y * v1.y)
        val v2Mag = Math.sqrt(v2.x * v2.x + v2.y * v2.y)

        val cos = dot / (v1Mag * v2Mag)
        return Math.toDegrees(Math.acos(cos.coerceIn(-1.0, 1.0)))
    }

    private fun perspectiveTransform(input: Mat, contour: MatOfPoint): Mat {
        val points = contour.toArray()
        val sortedPoints = sortPoints(points)

        // 여백을 완전히 제거하기 위해 직접적인 크롭 적용
        val width = input.width()
        val height = input.height()

        // 문서 영역만 정확하게 크롭
        val margin = 0 // 여백 완전 제거
        val src = MatOfPoint2f(*sortedPoints)
        val dst = MatOfPoint2f(
            Point(margin.toDouble(), margin.toDouble()),
            Point((width - margin).toDouble(), margin.toDouble()),
            Point((width - margin).toDouble(), (height - margin).toDouble()),
            Point(margin.toDouble(), (height - margin).toDouble())
        )

        val transform = Imgproc.getPerspectiveTransform(src, dst)
        val result = Mat()

        // 결과 이미지 크기를 문서 크기에 맞춤
        Imgproc.warpPerspective(
            input,
            result,
            transform,
            Size(width.toDouble(), height.toDouble())
        )

        // 추가적인 크롭 적용
        val cropRect = Rect(margin, margin, width - (2 * margin), height - (2 * margin))
        return Mat(result, cropRect)
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